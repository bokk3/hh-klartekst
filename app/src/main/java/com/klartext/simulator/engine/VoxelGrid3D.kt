package com.klartext.simulator.engine

import androidx.compose.ui.graphics.Color
import com.klartext.simulator.model.MoveType
import com.klartext.simulator.model.Point3D
import com.klartext.simulator.model.StockBox
import com.klartext.simulator.model.ToolGeometryType
import com.klartext.simulator.model.ToolpathSegment
import kotlin.math.*

/**
 * 3D Triangle produced by Marching Cubes mesh extraction.
 */
data class VoxelTriangle3D(
    val p1: Point3D,
    val p2: Point3D,
    val p3: Point3D,
    val normal: Point3D,
    val color: Color,
    val toolId: Int = 0
)

/**
 * Solid 3D workpiece mesh reconstructed from the 3D voxel grid.
 */
data class VoxelMesh3D(
    val triangles: List<VoxelTriangle3D>,
    val totalVoxelsCut: Int,
    val collisionDetected: Boolean,
    val collisionMessage: String? = null
)

/**
 * Plan for isotropic voxel grid spacing within budget.
 * Ported directly from Slavomrkva's tnc-sim planLiveVoxelGrid / planVoxelGrid.
 */
data class VoxelGridPlan(
    val w: Float,
    val d: Float,
    val h: Float,
    val cell: Float,
    val nx: Int,
    val ny: Int,
    val nz: Int,
    val total: Int,
    val dx: Float,
    val dy: Float,
    val dz: Float
) {
    companion object {
        const val LIVE_VOXEL_BUDGET = 450_000 // Optimized for 60fps Android execution
        const val REFINE_VOXEL_BUDGET = 1_200_000

        fun planLive(w: Float, d: Float, h: Float, budget: Int = LIVE_VOXEL_BUDGET): VoxelGridPlan {
            val safeW = w.coerceAtLeast(1f)
            val safeD = d.coerceAtLeast(1f)
            val safeH = h.coerceAtLeast(1f)
            val maxDim = max(safeW, max(safeD, safeH))

            val targetResolution = 90
            val cellCap = 1.0f
            val detailCell = min(maxDim / (targetResolution - 1), cellCap).coerceAtLeast(0.1f)

            fun atCell(c: Float): VoxelGridPlan {
                val nx = max(4, (safeW / c).roundToInt() + 1)
                val ny = max(4, (safeD / c).roundToInt() + 1)
                val nz = max(4, (safeH / c).roundToInt() + 1)
                val dx = safeW / (nx - 1)
                val dy = safeD / (ny - 1)
                val dz = safeH / (nz - 1)
                return VoxelGridPlan(safeW, safeD, safeH, c, nx, ny, nz, nx * ny * nz, dx, dy, dz)
            }

            var plan = atCell(detailCell)
            if (plan.total > budget) {
                var low = detailCell
                var high = maxDim
                for (i in 0 until 40) {
                    val mid = (low + high) / 2f
                    if (atCell(mid).total > budget) {
                        low = mid
                    } else {
                        high = mid
                    }
                }
                plan = atCell(high)
            }
            return plan
        }
    }
}

/**
 * Native 3D Voxel Stock Simulator with physical tool cutting and Marching Cubes solid reconstruction.
 * Ported from tnc-sim (Slavomrkva).
 */
class VoxelGrid3D(
    val plan: VoxelGridPlan,
    val ox: Float,
    val oy: Float,
    val oz: Float,
    val cylStockRadius: Float? = null,
    val cylCenterX: Float = 0f,
    val cylCenterY: Float = 0f
) {
    val nx = plan.nx
    val ny = plan.ny
    val nz = plan.nz
    val dx = plan.dx
    val dy = plan.dy
    val dz = plan.dz
    val maxZ = oz + plan.h

    val grid = ByteArray(nx * ny * nz)
    val cut = ByteArray(nx * ny * nz)

    var hasCut = false
    var collisionDetected = false
    var collisionMessage: String? = null
    var cutVoxelCount = 0

    init {
        reset()
    }

    fun reset() {
        cutVoxelCount = 0
        hasCut = false
        collisionDetected = false
        collisionMessage = null

        val rCyl = cylStockRadius
        val rCylSq = if (rCyl != null) (rCyl - dx) * (rCyl - dx) else 0f

        for (iz in 0 until nz) {
            val zIdx = iz * ny * nx
            for (iy in 0 until ny) {
                val yIdx = zIdx + iy * nx
                for (ix in 0 until nx) {
                    val border = (ix == 0 || ix == nx - 1 || iy == 0 || iy == ny - 1 || iz == 0 || iz == nz - 1)
                    var inShape = true
                    if (!border && rCyl != null) {
                        val wx = ox + ix * dx
                        val wy = oy + iy * dy
                        val ddx = wx - cylCenterX
                        val ddy = wy - cylCenterY
                        inShape = (ddx * ddx + ddy * ddy) <= rCylSq
                    }
                    val idx = yIdx + ix
                    grid[idx] = if (border || !inShape) 0 else 1
                    cut[idx] = 0
                }
            }
        }
    }

    /**
     * Shared physical profile: cutting and collision agree at every Z slice.
     * Ported from tnc-sim vxToolRadiusAtHeight().
     */
    fun vxToolRadiusAtHeight(
        toolR: Float,
        geometry: ToolGeometryType,
        dz2: Float,
        cornerR: Float = 0f,
        pointAngleDeg: Float = 180f,
        lcuts: Float = 50f
    ): Float {
        if (dz2 < 0f) return 0f
        return when (geometry) {
            ToolGeometryType.BALL_NOSE -> {
                val r2 = if (cornerR > 0f) cornerR else toolR
                if (dz2 < r2) {
                    val frac = (dz2 / r2).coerceIn(0f, 1f)
                    r2 * sqrt(max(0f, 1f - (1f - frac) * (1f - frac)))
                } else {
                    toolR
                }
            }
            ToolGeometryType.TORUS_MILL -> {
                val r2 = if (cornerR > 0f) cornerR else 0f
                val core = max(0f, toolR - r2)
                if (dz2 < r2) {
                    core + sqrt(max(0f, r2 * r2 - (r2 - dz2) * (r2 - dz2)))
                } else {
                    toolR
                }
            }
            ToolGeometryType.SPOT_DRILL_90, ToolGeometryType.TWIST_DRILL -> {
                val hr2 = Math.toRadians((pointAngleDeg / 2.0)).toFloat()
                min(toolR, dz2 * tan(hr2))
            }
            ToolGeometryType.CHAMFER_MILL_45 -> {
                val hr2 = Math.toRadians((pointAngleDeg / 2.0)).toFloat()
                val coneCap = lcuts * tan(hr2)
                min(coneCap, toolR + dz2 * tan(hr2))
            }
            else -> toolR
        }
    }

    /**
     * Cuts material along a single cutter center position (tx, ty, tz).
     */
    fun vxCut(
        tx: Float,
        ty: Float,
        tz: Float,
        toolR: Float,
        geometry: ToolGeometryType,
        cornerR: Float = 0f,
        pointAngleDeg: Float = 180f,
        lcuts: Float = 50f,
        toolNum: Int = 1
    ) {
        if (tz > maxZ) return

        var maxR = toolR
        if (geometry == ToolGeometryType.CHAMFER_MILL_45) {
            val hr = Math.toRadians((pointAngleDeg / 2.0)).toFloat()
            val coneMaxScan = lcuts * tan(hr)
            maxR = max(toolR, coneMaxScan)
        }

        val ixMin = max(0, floor((tx - maxR - ox) / dx).toInt() - 1)
        val ixMax = min(nx - 1, ceil((tx + maxR - ox) / dx).toInt() + 1)
        val iyMin = max(0, floor((ty - maxR - oy) / dy).toInt() - 1)
        val iyMax = min(ny - 1, ceil((ty + maxR - oy) / dy).toInt() + 1)
        val izMin = max(0, floor((tz - oz) / dz).toInt() - 1)
        val izMax = nz - 1

        val lcutsZ = lcuts

        for (iz in izMin..izMax) {
            val pz = oz + iz * dz
            if (pz < tz - dz * 0.5f) continue
            val dz2 = pz - tz
            val effR = vxToolRadiusAtHeight(toolR, geometry, dz2, cornerR, pointAngleDeg, lcuts)
            val r2 = effR * effR
            val zIdx = iz * ny * nx

            for (iy in iyMin..iyMax) {
                val py = oy + iy * dy
                val ddy = py - ty
                val ddy2 = ddy * ddy
                val yIdx = zIdx + iy * nx

                for (ix in ixMin..ixMax) {
                    val px = ox + ix * dx
                    val ddx = px - tx
                    if (ddx * ddx + ddy2 <= r2) {
                        val gi = yIdx + ix
                        if (grid[gi].toInt() != 0) {
                            grid[gi] = 0
                            val isShankCollision = dz2 > lcutsZ
                            cut[gi] = if (isShankCollision) {
                                collisionDetected = true
                                collisionMessage = "⚠ SCHACHTBOTSING: Fuisdiepte overschrijdt spaangroeflengte LCUTS (${lcuts.toInt()}mm) bij Z=${tz.roundToInt()}"
                                255.toByte()
                            } else {
                                (toolNum.coerceIn(1, 8)).toByte()
                            }
                            cutVoxelCount++
                            hasCut = true
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if tool geometry intersects existing uncarved material.
     */
    fun vxCollides(
        tx: Float,
        ty: Float,
        tz: Float,
        toolR: Float,
        geometry: ToolGeometryType,
        cornerR: Float = 0f,
        pointAngleDeg: Float = 180f,
        lcuts: Float = 50f
    ): Boolean {
        var maxR = toolR
        if (geometry == ToolGeometryType.CHAMFER_MILL_45) {
            val hr = Math.toRadians((pointAngleDeg / 2.0)).toFloat()
            maxR = max(toolR, lcuts * tan(hr))
        }

        val ixMin = max(0, floor((tx - maxR - ox) / dx).toInt())
        val ixMax = min(nx - 1, ceil((tx + maxR - ox) / dx).toInt())
        val iyMin = max(0, floor((ty - maxR - oy) / dy).toInt())
        val iyMax = min(ny - 1, ceil((ty + maxR - oy) / dy).toInt())
        val izMin = max(0, floor((tz - oz) / dz).toInt())
        val izMax = nz - 1

        for (iz in izMin..izMax) {
            val pz = oz + iz * dz
            if (pz < tz) continue
            val radius = vxToolRadiusAtHeight(toolR, geometry, pz - tz, cornerR, pointAngleDeg, lcuts)
            val r2 = radius * radius
            val zIdx = iz * ny * nx

            for (iy in iyMin..iyMax) {
                val py = oy + iy * dy
                val ddy = py - ty
                val ddy2 = ddy * ddy
                val yIdx = zIdx + iy * nx

                for (ix in ixMin..ixMax) {
                    val px = ox + ix * dx
                    val ddx = px - tx
                    if (ddx * ddx + ddy2 <= r2) {
                        if (grid[yIdx + ix].toInt() != 0) return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Carves a toolpath segment with swept tool motion.
     */
    fun carveSegment(seg: ToolpathSegment) {
        val dl = seg.dl
        val toolR = seg.effectiveRadius
        val geometry = seg.toolGeometry
        val cornerR = seg.toolCornerRadius
        val angle = seg.pointAngleDeg
        val lcuts = 45f // default flute length

        val isRapid = seg.moveType == MoveType.RAPID
        val spindleOn = seg.spindleSpeed > 0f

        val start = seg.start
        val end = seg.end
        val dist = start.distanceTo(end)

        val cellSize = min(dx, min(dy, dz))

        // 1. Rapid move or stopped spindle collision check
        if (isRapid || !spindleOn) {
            val steps = max(2, ceil(dist / cellSize).toInt())
            for (si in 0..steps) {
                val t = si.toFloat() / steps
                val cx = start.x + (end.x - start.x) * t
                val cy = start.y + (end.y - start.y) * t
                val cz = start.z + (end.z - start.z) * t + dl
                if (vxCollides(cx, cy, cz, toolR, geometry, cornerR, angle, lcuts)) {
                    collisionDetected = true
                    collisionMessage = "⚠ RAPID COLLISIE: Frees beweegt met FMAX of stilstaande spil door materiaal bij X=${cx.roundToInt()} Y=${cy.roundToInt()} Z=${cz.roundToInt()}"
                    break
                }
            }
            return
        }

        // 2. Active cutting motion
        val steps = max(2, ceil((dist / cellSize) * 1.4f).toInt())
        for (si in 0..steps) {
            val t = si.toFloat() / steps
            val cx = start.x + (end.x - start.x) * t
            val cy = start.y + (end.y - start.y) * t
            val cz = start.z + (end.z - start.z) * t + dl
            vxCut(cx, cy, cz, toolR, geometry, cornerR, angle, lcuts, seg.toolNumber)
        }
    }

    /**
     * Marching Cubes Solid Surface Extraction.
     * Ported directly from tnc-sim vxBuildGeometryRange().
     */
    fun generateMesh(): VoxelMesh3D {
        val triangles = mutableListOf<VoxelTriangle3D>()
        val edgeCorners = MarchingCubesTables.VX_EDGE_CORNERS
        val edgeTable = MarchingCubesTables.MC_EDGE_TABLE
        val triTable = MarchingCubesTables.MC_TRI_TABLE
        val toolColors = MarchingCubesTables.TOOL_CUT_COLORS

        val stockColor = Color(0xFF9A9DA6) // Slavomrkva stock hex #9a9da6

        // Key directional light vector from Slavomrkva: L = (1.2, -0.8, 2.0) normalized
        val lx = 1.2f
        val ly = -0.8f
        val lz = 2.0f
        val lLen = sqrt(lx * lx + ly * ly + lz * lz)
        val nlx = lx / lLen
        val nly = ly / lLen
        val nlz = lz / lLen

        fun vInterp(iso: Float, p1x: Float, p1y: Float, p1z: Float, v1: Float, p2x: Float, p2y: Float, p2z: Float, v2: Float): Point3D {
            if (abs(v1 - v2) < 1e-5f) return Point3D(p1x, p1y, p1z)
            val t = (iso - v1) / (v2 - v1)
            return Point3D(p1x + t * (p2x - p1x), p1y + t * (p2y - p1y), p1z + t * (p2z - p1z))
        }

        val ep = Array(12) { Point3D() }
        val cornerX = FloatArray(8)
        val cornerY = FloatArray(8)
        val cornerZ = FloatArray(8)
        val values = FloatArray(8)

        fun valAt(ix: Int, iy: Int, iz: Int): Float {
            if (ix !in 0 until nx || iy !in 0 until ny || iz !in 0 until nz) return 0f
            return grid[iz * ny * nx + iy * nx + ix].toFloat()
        }

        val iso = 0.5f

        for (iz in 0 until nz - 1) {
            val z0 = oz + iz * dz
            val z1 = z0 + dz

            for (iy in 0 until ny - 1) {
                val y0 = oy + iy * dy
                val y1 = y0 + dy

                for (ix in 0 until nx - 1) {
                    val x0 = ox + ix * dx
                    val x1 = x0 + dx

                    values[0] = valAt(ix, iy, iz)
                    values[1] = valAt(ix + 1, iy, iz)
                    values[2] = valAt(ix + 1, iy + 1, iz)
                    values[3] = valAt(ix, iy + 1, iz)
                    values[4] = valAt(ix, iy, iz + 1)
                    values[5] = valAt(ix + 1, iy, iz + 1)
                    values[6] = valAt(ix + 1, iy + 1, iz + 1)
                    values[7] = valAt(ix, iy + 1, iz + 1)

                    var ci = 0
                    if (values[0] > iso) ci = ci or 1
                    if (values[1] > iso) ci = ci or 2
                    if (values[2] > iso) ci = ci or 4
                    if (values[3] > iso) ci = ci or 8
                    if (values[4] > iso) ci = ci or 16
                    if (values[5] > iso) ci = ci or 32
                    if (values[6] > iso) ci = ci or 64
                    if (values[7] > iso) ci = ci or 128

                    if (ci == 0 || ci == 255) continue

                    val et = edgeTable[ci]
                    cornerX[0] = x0; cornerY[0] = y0; cornerZ[0] = z0
                    cornerX[1] = x1; cornerY[1] = y0; cornerZ[1] = z0
                    cornerX[2] = x1; cornerY[2] = y1; cornerZ[2] = z0
                    cornerX[3] = x0; cornerY[3] = y1; cornerZ[3] = z0
                    cornerX[4] = x0; cornerY[4] = y0; cornerZ[4] = z1
                    cornerX[5] = x1; cornerY[5] = y0; cornerZ[5] = z1
                    cornerX[6] = x1; cornerY[6] = y1; cornerZ[6] = z1
                    cornerX[7] = x0; cornerY[7] = y1; cornerZ[7] = z1

                    for (e in 0 until 12) {
                        if ((et and (1 shl e)) != 0) {
                            val ea = edgeCorners[e][0]
                            val eb = edgeCorners[e][1]
                            ep[e] = vInterp(iso, cornerX[ea], cornerY[ea], cornerZ[ea], values[ea], cornerX[eb], cornerY[eb], cornerZ[eb], values[eb])
                        }
                    }

                    val triBase = ci * 16
                    for (ti in 0 until 15 step 3) {
                        val e0 = triTable[triBase + ti]
                        if (e0 < 0) break
                        val pa = ep[e0]
                        val pb = ep[triTable[triBase + ti + 1]]
                        val pc = ep[triTable[triBase + ti + 2]]

                        // Normal
                        val ax = pb.x - pa.x; val ay = pb.y - pa.y; val az = pb.z - pa.z
                        val bx = pc.x - pa.x; val by = pc.y - pa.y; val bz = pc.z - pa.z
                        var nx2 = ay * bz - az * by
                        var ny2 = az * bx - ax * bz
                        var nz2 = ax * by - ay * bx
                        val nl = sqrt(nx2 * nx2 + ny2 * ny2 + nz2 * nz2).coerceAtLeast(1e-6f)
                        nx2 /= nl; ny2 /= nl; nz2 /= nl

                        // Color determination: sample center
                        val cxAvg = (pa.x + pb.x + pc.x) / 3f
                        val cyAvg = (pa.y + pb.y + pc.y) / 3f
                        val czAvg = (pa.z + pb.z + pc.z) / 3f

                        val cutX = ((cxAvg - ox) / dx).roundToInt().coerceIn(0, nx - 1)
                        val cutY = ((cyAvg - oy) / dy).roundToInt().coerceIn(0, ny - 1)
                        val cutZ = ((czAvg - oz) / dz).roundToInt().coerceIn(0, nz - 1)

                        val isCut = cut[cutZ * ny * nx + cutY * nx + cutX].toInt() and 0xFF

                        var baseColor = stockColor
                        if (isCut == 255) {
                            baseColor = Color(0xFFFF3333) // Collision warning red
                        } else if (isCut in 1..8) {
                            val tc = toolColors[(isCut - 1) % toolColors.size]
                            baseColor = Color(tc[0], tc[1], tc[2])
                        }

                        // Lambertian Shading with key directional light
                        val dot = max(0f, nx2 * nlx + ny2 * nly + nz2 * nlz)
                        val shade = (0.42f + 0.58f * dot).coerceIn(0.2f, 1.0f)
                        val shadedColor = Color(
                            red = (baseColor.red * shade).coerceIn(0f, 1f),
                            green = (baseColor.green * shade).coerceIn(0f, 1f),
                            blue = (baseColor.blue * shade).coerceIn(0f, 1f),
                            alpha = 1.0f
                        )

                        triangles.add(
                            VoxelTriangle3D(
                                p1 = pa,
                                p2 = pb,
                                p3 = pc,
                                normal = Point3D(nx2, ny2, nz2),
                                color = shadedColor,
                                toolId = isCut
                            )
                        )
                    }
                }
            }
        }

        return VoxelMesh3D(
            triangles = triangles,
            totalVoxelsCut = cutVoxelCount,
            collisionDetected = collisionDetected,
            collisionMessage = collisionMessage
        )
    }

    companion object {
        fun buildFromStock(stock: StockBox): VoxelGrid3D {
            val plan = VoxelGridPlan.planLive(stock.width, stock.height, stock.depth)
            return VoxelGrid3D(
                plan = plan,
                ox = stock.min.x,
                oy = stock.min.y,
                oz = stock.min.z
            )
        }
    }
}
