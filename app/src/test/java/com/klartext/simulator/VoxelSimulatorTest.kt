package com.klartext.simulator

import com.klartext.simulator.engine.MarchingCubesTables
import com.klartext.simulator.engine.VoxelGrid3D
import com.klartext.simulator.engine.VoxelGridPlan
import com.klartext.simulator.model.MoveType
import com.klartext.simulator.model.Point3D
import com.klartext.simulator.model.StockBox
import com.klartext.simulator.model.ToolGeometryType
import com.klartext.simulator.model.ToolpathSegment
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class VoxelSimulatorTest {

    @Test
    fun testMarchingCubesTablesIntegrity() {
        assertEquals("Edge table must have 256 entries", 256, MarchingCubesTables.MC_EDGE_TABLE.size)
        assertEquals("Tri table must have 4096 entries", 4096, MarchingCubesTables.MC_TRI_TABLE.size)
        assertEquals("Edge corners must have 12 edges", 12, MarchingCubesTables.VX_EDGE_CORNERS.size)
        assertEquals("Tool cut colors must have 8 colors", 8, MarchingCubesTables.TOOL_CUT_COLORS.size)
    }

    @Test
    fun testVoxelGridPlannerIsotropic() {
        val plan = VoxelGridPlan.planLive(100f, 100f, 30f)
        assertTrue("Grid nx must be at least 4", plan.nx >= 4)
        assertTrue("Grid ny must be at least 4", plan.ny >= 4)
        assertTrue("Grid nz must be at least 4", plan.nz >= 4)
        assertTrue("Total voxels within budget", plan.total <= VoxelGridPlan.LIVE_VOXEL_BUDGET)
        assertTrue("dx and dy should be nearly equal for square stock", abs(plan.dx - plan.dy) < 0.1f)
    }

    @Test
    fun testBallNoseToolProfile() {
        val stock = StockBox(Point3D(0f, 0f, -20f), Point3D(50f, 50f, 0f))
        val grid = VoxelGrid3D.buildFromStock(stock)

        val toolR = 5f
        val r2 = 5f // full ball nose

        // At tip (dz = 0), effective radius must be 0
        val rTip = grid.vxToolRadiusAtHeight(toolR, ToolGeometryType.BALL_NOSE, 0f, cornerR = r2)
        assertEquals(0f, rTip, 0.001f)

        // At equator (dz = r2 = 5), effective radius must be toolR = 5
        val rEquator = grid.vxToolRadiusAtHeight(toolR, ToolGeometryType.BALL_NOSE, 5f, cornerR = r2)
        assertEquals(5f, rEquator, 0.001f)

        // At dz = 2.5: r2 * sqrt(1 - (1 - 0.5)^2) = 5 * sqrt(0.75) ≈ 4.33
        val rMid = grid.vxToolRadiusAtHeight(toolR, ToolGeometryType.BALL_NOSE, 2.5f, cornerR = r2)
        assertEquals(5f * sqrt(0.75f), rMid, 0.05f)

        // Above ball sphere (dz > 5), effective radius remains toolR = 5
        val rCylinder = grid.vxToolRadiusAtHeight(toolR, ToolGeometryType.BALL_NOSE, 8f, cornerR = r2)
        assertEquals(5f, rCylinder, 0.001f)
    }

    @Test
    fun testTorusMillToolProfile() {
        val stock = StockBox(Point3D(0f, 0f, -20f), Point3D(50f, 50f, 0f))
        val grid = VoxelGrid3D.buildFromStock(stock)

        val toolR = 10f
        val r2 = 2f // bull-nose with 2mm corner radius
        val core = 8f

        // At tip (dz = 0), effective radius is core = 8
        val rTip = grid.vxToolRadiusAtHeight(toolR, ToolGeometryType.TORUS_MILL, 0f, cornerR = r2)
        assertEquals(8f, rTip, 0.001f)

        // At dz = 2 (corner zone ends), effective radius is toolR = 10
        val rEndCorner = grid.vxToolRadiusAtHeight(toolR, ToolGeometryType.TORUS_MILL, 2f, cornerR = r2)
        assertEquals(10f, rEndCorner, 0.001f)
    }

    @Test
    fun testDrillConeProfile() {
        val stock = StockBox(Point3D(0f, 0f, -20f), Point3D(50f, 50f, 0f))
        val grid = VoxelGrid3D.buildFromStock(stock)

        val toolR = 5f
        val angle = 118f // standard twist drill

        // At tip, radius is 0
        val rTip = grid.vxToolRadiusAtHeight(toolR, ToolGeometryType.TWIST_DRILL, 0f, pointAngleDeg = angle)
        assertEquals(0f, rTip, 0.001f)

        // Radius grows with tan(angle/2) up to toolR
        val rPart = grid.vxToolRadiusAtHeight(toolR, ToolGeometryType.TWIST_DRILL, 1.5f, pointAngleDeg = angle)
        assertTrue(rPart > 0f && rPart <= toolR)

        // Far above point, capped at toolR
        val rFull = grid.vxToolRadiusAtHeight(toolR, ToolGeometryType.TWIST_DRILL, 20f, pointAngleDeg = angle)
        assertEquals(toolR, rFull, 0.001f)
    }

    @Test
    fun testCarveSegmentAndMarchingCubesMesh() {
        val stock = StockBox(Point3D(0f, 0f, -10f), Point3D(40f, 40f, 0f))
        val voxelGrid = VoxelGrid3D.buildFromStock(stock)

        // Carve slot from (10, 20, -3) to (30, 20, -3) with 5mm radius mill
        val seg = ToolpathSegment.Linear(
            lineNumber = 1,
            start = Point3D(10f, 20f, -3f),
            end = Point3D(30f, 20f, -3f),
            moveType = MoveType.FEED,
            feedRate = 500f,
            spindleSpeed = 4000f,
            toolNumber = 1,
            toolRadius = 5f,
            effectiveRadius = 5f
        )

        voxelGrid.carveSegment(seg)
        assertTrue("Material must be cut", voxelGrid.cutVoxelCount > 0)
        assertTrue("hasCut flag should be true", voxelGrid.hasCut)

        val mesh = voxelGrid.generateMesh()
        assertTrue("Marching cubes should produce triangles", mesh.triangles.isNotEmpty())
        assertEquals("No collision on normal cut", false, mesh.collisionDetected)

        // Check that at least some triangles have tool ID = 1
        val cutTriangles = mesh.triangles.filter { it.toolId == 1 }
        assertTrue("At least one triangle should carry tool cut color", cutTriangles.isNotEmpty())
    }

    @Test
    fun testToolShankCollisionDetection() {
        val stock = StockBox(Point3D(0f, 0f, -30f), Point3D(40f, 40f, 0f))
        val voxelGrid = VoxelGrid3D.buildFromStock(stock)

        // Plunge deep beyond LCUTS (LCUTS = 45mm, stock depth 30mm)
        // Explicitly cut with small LCUTS = 5mm to trigger shank collision
        voxelGrid.vxCut(
            tx = 20f,
            ty = 20f,
            tz = -25f,
            toolR = 5f,
            geometry = ToolGeometryType.CYLINDRICAL_ENDMILL,
            lcuts = 5f, // only 5mm cutting flute, cutting down to -25 with stock top 0 = 25mm deep!
            toolNum = 1
        )

        assertTrue("Shank collision must be detected", voxelGrid.collisionDetected)
        assertNotNull("Collision message must be set", voxelGrid.collisionMessage)
    }

    @Test
    fun testRapidCollisionDetection() {
        val stock = StockBox(Point3D(0f, 0f, -20f), Point3D(40f, 40f, 0f))
        val voxelGrid = VoxelGrid3D.buildFromStock(stock)

        // Rapid move traversing through stock at Z = -10
        val rapidSeg = ToolpathSegment.Linear(
            lineNumber = 2,
            start = Point3D(5f, 20f, -10f),
            end = Point3D(35f, 20f, -10f),
            moveType = MoveType.RAPID,
            feedRate = 15000f,
            spindleSpeed = 4000f,
            toolNumber = 1,
            toolRadius = 5f,
            effectiveRadius = 5f
        )

        voxelGrid.carveSegment(rapidSeg)
        assertTrue("Rapid move through material must trigger collision alert", voxelGrid.collisionDetected)
        assertTrue(voxelGrid.collisionMessage?.contains("RAPID COLLISIE") == true)
    }
}
