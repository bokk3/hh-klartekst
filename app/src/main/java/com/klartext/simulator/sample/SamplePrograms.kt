package com.klartext.simulator.sample

object SamplePrograms {

    /**
     * 1. CONTOURFREZEN_V4.H:
     * Omtrekfrezen (120x120) + complex eiland met CC/C, CR, CT, RND en CHF 10,
     * inclusief voorfrezen (DR+0.5), nafrezen (DR+0) en ontbramen (KANTENFREES6 met DL-1.5 DR-1.5).
     */
    val contourfrezenV4 = """
        0 BEGIN PGM CONTOURFREZEN_V4 MM
        1 ; --- RUWDEEL DEFINITIE ---
        2 BLK FORM 0.1 Z X+0 Y+0 Z-25
        3 BLK FORM 0.2 X+120 Y+120 Z+0
        4 ; ===================================================
        5 ; GEREEDSCHAP 1: VOORFREZEN CONTOUR (OVERMAAT DR+0.5)
        6 ; ===================================================
        7 TOOL CALL "FREES16" Z S3500 F1200 DR+0.5
        8 L Z+100 R0 FMAX
        9 L X-20 Y-20 R0 FMAX
        10 L Z+2 R0 FMAX
        11 L Z-10 R0 F500 M3
        12 APPR LCT X+15 Y+15 R10 RL F800
        13 L X+105 Y+15
        14 RND R12
        15 L X+105 Y+65
        16 CR X+75 Y+95 R+30 DR+
        17 CT X+45 Y+105
        18 CHF 10
        19 L X+15 Y+105
        20 L X+15 Y+15
        21 DEP LCT X-20 Y-20 R10 F1000
        22 L Z+50 R0 FMAX
        23 ; ===================================================
        24 ; GEREEDSCHAP 2: NAFREZEN CONTOUR OP MAAT (DR+0)
        25 ; ===================================================
        26 TOOL CALL "FREES16" Z S4200 F900 DR+0
        27 L X-20 Y-20 R0 FMAX
        28 L Z+2 R0 FMAX
        29 L Z-10 R0 F400 M3
        30 APPR LCT X+15 Y+15 R10 RL F900
        31 L X+105 Y+15
        32 RND R12
        33 L X+105 Y+65
        34 CR X+75 Y+95 R+30 DR+
        35 CT X+45 Y+105
        36 CHF 10
        37 L X+15 Y+105
        38 L X+15 Y+15
        39 DEP LCT X-20 Y-20 R10 F1000
        40 L Z+100 R0 FMAX
        41 ; ===================================================
        42 ; GEREEDSCHAP 3: ONTBRAMEN 0.3x45 MET KANTENFREES6
        43 ; ===================================================
        44 TOOL CALL "KANTENFREES6" Z S5000 F1500 DL-1.5 DR-1.5
        45 L X-20 Y-20 R0 FMAX
        46 L Z+2 R0 FMAX
        47 L Z-0.3 R0 F600 M3
        48 APPR LCT X+15 Y+15 R5 RL F1500
        49 L X+105 Y+15
        50 RND R12
        51 L X+105 Y+65
        52 CR X+75 Y+95 R+30 DR+
        53 CT X+45 Y+105
        54 CHF 10
        55 L X+15 Y+105
        56 L X+15 Y+15
        57 DEP LCT X-20 Y-20 R5 F1500
        58 L Z+100 R0 FMAX M30
        59 END PGM CONTOURFREZEN_V4 MM
    """.trimIndent()

    /**
     * 2. BOOR_FREESCYCLI.H:
     * Vlakken, kamers/sleuven (CYCL DEF 251, 252, 253), poolcoördinaten (CC + LP PR PA) en gatenraster.
     */
    val boorFreescycli = """
        0 BEGIN PGM BOOR_FREESCYCLI MM
        1 BLK FORM 0.1 Z X+0 Y+0 Z-30
        2 BLK FORM 0.2 X+140 Y+100 Z+0
        3 ; --- 1. VLAKFREZEN OP Z=0 ---
        4 TOOL CALL "VLAKFREES63" Z S2200 F1500
        5 L Z+50 R0 FMAX
        6 L X-40 Y+50 R0 FMAX M3
        7 L Z+0 R0 F800
        8 L X+180 Y+50 F1500
        9 L Z+50 R0 FMAX
        10 ; --- 2. RECHTHOEKIGE KAMER (CYCL DEF 251) ---
        11 TOOL CALL "FREES16" Z S3200 F900
        12 CYCL DEF 251 RECHTHOEKIGE KAMER ~
           Q215=+0 ;BEWERKINGSOMVANG ~
           Q218=+60 ;1E ZIJDELENGTE ~
           Q219=+45 ;2E ZIJDELENGTE ~
           Q220=+8 ;HOEKRADIUS ~
           Q368=+0.2 ;OVERMAAT ZIJKANT ~
           Q224=+0 ;DRAAIHOEK ~
           Q367=+0 ;KAMERPOSITIE (MIDDEN) ~
           Q207=+800 ;FREESVOEDING ~
           Q351=+1 ;FREESWIJZE (MEELOPEND) ~
           Q201=-12 ;DIEPTE ~
           Q202=+6 ;AANZETDIEPTE ~
           Q369=+0.1 ;OVERMAAT DIEPTE ~
           Q206=+300 ;AANZETVOEDING DIEPTE ~
           Q338=+0 ;AANZET NABEWERKEN ~
           Q200=+2 ;VEILIGHEIDSAFST. ~
           Q203=+0 ;COORD. OPPERVLAK ~
           Q204=+50 ;2E VEILIGHEIDSAFST. ~
           Q370=+1.2 ;BAANOVERLAPPING
        13 L X+40 Y+50 R0 FMAX M3
        14 CYCL CALL
        15 ; --- 3. RONDE KAMER MET POOLCOORDINATEN (CYCL DEF 252) ---
        16 CYCL DEF 252 RONDE KAMER ~
           Q215=+0 ;BEWERKINGSOMVANG ~
           Q223=+36 ;CIRKELDIAMETER ~
           Q368=+0 ;OVERMAAT ZIJKANT ~
           Q207=+750 ;FREESVOEDING ~
           Q351=+1 ;MEELOPEND ~
           Q201=-15 ;DIEPTE ~
           Q202=+5 ;AANZETDIEPTE ~
           Q369=+0 ;OVERMAAT DIEPTE ~
           Q206=+250 ;VOEDING DIEPTE ~
           Q338=+0 ;AANZET NABEWERKEN ~
           Q200=+2 ;VEILIGHEIDSAFST. ~
           Q203=+0 ;COORD. OPPERVLAK ~
           Q204=+50 ;2E VEILIGHEIDSAFST. ~
           Q370=+1.2 ;BAANOVERLAPPING
        17 L X+105 Y+50 R0 FMAX
        18 CYCL CALL
        19 ; --- 4. SLEUF (CYCL DEF 253) ---
        20 CYCL DEF 253 SLEUFFREZEN ~
           Q215=+0 ;BEWERKINGSOMVANG ~
           Q218=+40 ;SLEUFLENGTE ~
           Q219=+14 ;SLEUFBREEDTE ~
           Q368=+0 ;OVERMAAT ZIJKANT ~
           Q374=+0 ;DRAAIHOEK ~
           Q367=+0 ;SLEUFPOSITIE (MIDDEN) ~
           Q207=+600 ;FREESVOEDING ~
           Q351=+1 ;MEELOPEND ~
           Q201=-8 ;DIEPTE ~
           Q202=+4 ;AANZETDIEPTE ~
           Q369=+0 ;OVERMAAT DIEPTE ~
           Q206=+200 ;AANZETVOEDING DIEPTE ~
           Q338=+0 ;AANZET NABEWERKEN ~
           Q200=+2 ;VEILIGHEIDSAFST. ~
           Q203=+0 ;COORD. OPPERVLAK ~
           Q204=+50 ;2E VEILIGHEIDSAFST.
        21 L X+40 Y+85 R0 FMAX
        22 CYCL CALL
        23 L Z+100 R0 FMAX M30
        24 END PGM BOOR_FREESCYCLI MM
    """.trimIndent()

    /**
     * 3. KAMERSFREZEN_H5.H:
     * Blok 130x90x25 met kamers, sleuven en 3× M8 schroefdraadcyclus (centreren, boren D6.8, tappen M8).
     */
    val kamersfrezenH5 = """
        0 BEGIN PGM KAMERSFREZEN_H5 MM
        1 BLK FORM 0.1 Z X+0 Y+0 Z-25
        2 BLK FORM 0.2 X+130 Y+90 Z+0
        3 ; --- GATENPATROON 3x M8 (X=25, X=65, X=105 bij Y=20) ---
        4 ; 1. CENTREREN 90° (D12)
        5 TOOL CALL "CENTERBOOR" Z S1800 F200
        6 CYCL DEF 240 CENTREREN ~
           Q200=+2 ;VEILIGHEIDSAFST. ~
           Q343=+0 ;SELECTIE DIEPTE/DIAMETER ~
           Q201=-3.5 ;DIEPTE ~
           Q206=+150 ;AANZETVOEDING DIEPTE ~
           Q211=+0.1 ;STILSTANDTIJD ~
           Q203=+0 ;COORD. OPPERVLAK ~
           Q204=+50 ;2E VEILIGHEIDSAFST.
        7 L X+25 Y+20 R0 FMAX M3
        8 CYCL CALL
        9 L X+65 Y+20 R0 FMAX
        10 CYCL CALL
        11 L X+105 Y+20 R0 FMAX
        12 CYCL CALL
        13 ; 2. BOREN KERNGAT D6.8 VOOR M8
        14 TOOL CALL "VHMBOOR6.8" Z S2400 F280
        15 CYCL DEF 200 BOREN ~
           Q200=+2 ;VEILIGHEIDSAFST. ~
           Q201=-22 ;DIEPTE ~
           Q206=+280 ;AANZETVOEDING DIEPTE ~
           Q202=+6 ;AANZETDIEPTE ~
           Q210=+0 ;STILSTANDTIJD BOVEN ~
           Q203=+0 ;COORD. OPPERVLAK ~
           Q204=+50 ;2E VEILIGHEIDSAFST. ~
           Q211=+0.1 ;STILSTANDTIJD ONDER
        16 L X+25 Y+20 R0 FMAX M3
        17 CYCL CALL
        18 L X+65 Y+20 R0 FMAX
        19 CYCL CALL
        20 L X+105 Y+20 R0 FMAX
        21 CYCL CALL
        22 ; 3. TAPPEN M8 (S=300, F=S x 1.25 = 375 mm/min)
        23 TOOL CALL "TAPM8" Z S300 F375
        24 CYCL DEF 206 SCHROEFDRAAD TAPPEN ~
           Q200=+2 ;VEILIGHEIDSAFST. ~
           Q201=-16 ;DRAADDIEPTE ~
           Q206=+375 ;AANZETVOEDING (S x SPOED) ~
           Q211=+0 ;STILSTANDTIJD ~
           Q203=+0 ;COORD. OPPERVLAK ~
           Q204=+50 ;2E VEILIGHEIDSAFST.
        25 L X+25 Y+20 R0 FMAX M3
        26 CYCL CALL
        27 L X+65 Y+20 R0 FMAX
        28 CYCL CALL
        29 L X+105 Y+20 R0 FMAX
        30 CYCL CALL
        31 L Z+100 R0 FMAX M30
        32 END PGM KAMERSFREZEN_H5 MM
    """.trimIndent()

    /**
     * 4. U_VORM_LABEL.H:
     * Herhaling van een U-vormige kamer via nulpuntverplaatsing (CYCL DEF 7.0) en boorfrezen (CYCL DEF 208).
     */
    val uVormLabel = """
        0 BEGIN PGM U_VORM_LABEL MM
        1 BLK FORM 0.1 Z X+0 Y+0 Z-20
        2 BLK FORM 0.2 X+120 Y+100 Z+0
        3 ; --- HOOFDPROGRAMMA ---
        4 TOOL CALL "FREES10" Z S3600 F1000
        5 L Z+50 R0 FMAX M3
        6 ; POSITIE 1: ORIGINEEL NULPUNT
        7 CALL LBL "U_KAMER"
        8 ; POSITIE 2: NULPUNT VERPLAATSEN MET CYCLUS 7 (X+60)
        9 CYCL DEF 7.0 NULPUNT
        10 CYCL DEF 7.1 X+60
        11 CYCL DEF 7.2 Y+0
        12 CYCL DEF 7.3 Z+0
        13 CALL LBL "U_KAMER"
        14 ; NULPUNT RESETTEN
        15 CYCL DEF 7.0 NULPUNT
        16 CYCL DEF 7.1 X+0
        17 CYCL DEF 7.2 Y+0
        18 CYCL DEF 7.3 Z+0
        19 ; BOORFREZEN MET CYCLUS 208 IN HET MIDDEN (X=60 Y=75 D=22 mm)
        20 CYCL DEF 208 BOORFREZEN ~
           Q200=+2 ;VEILIGHEIDSAFST. ~
           Q201=-14 ;DIEPTE ~
           Q206=+450 ;AANZETVOEDING DIEPTE ~
           Q334=+1.5 ;AANZETDIEPTE PER WENTELING ~
           Q203=+0 ;COORD. OPPERVLAK ~
           Q204=+50 ;2E VEILIGHEIDSAFST. ~
           Q335=+22 ;NOMINALE DIAMETER ~
           Q342=+10 ;VOORBOORDIAMETER ~
           Q351=+1 ;FREESWIJZE MEELOPEND
        21 L X+60 Y+75 R0 FMAX
        22 CYCL CALL
        23 L Z+100 R0 FMAX M30
        24 ; =======================================
        25 ; SUBROUTINE: U-VORMIGE CONTOUR
        26 ; =======================================
        27 LBL "U_KAMER"
        28 L X+10 Y+15 R0 FMAX
        29 L Z+2 R0 FMAX
        30 L Z-6 R0 F400
        31 APPR LT X+15 Y+20 LEN5 RL F800
        32 L X+15 Y+55
        33 RND R5
        34 L X+30 Y+55
        35 RND R5
        36 L X+30 Y+35
        37 L X+40 Y+35
        38 L X+40 Y+55
        39 RND R5
        40 L X+50 Y+55
        41 RND R5
        42 L X+50 Y+20
        43 DEP LT LEN5 F800
        44 L Z+20 R0 FMAX
        45 LBL 0
        46 END PGM U_VORM_LABEL MM
    """.trimIndent()

    /**
     * Foutdemo 1: R0 vs. RL Valkuil in CONTOURFREZEN.
     * Regel 17 bevat per ongeluk 'R0' i.p.v. 'NO ENT', waardoor de frees over het werkstuk loopt!
     */
    val demoR0Pitfall = """
        0 BEGIN PGM FOUT_R0_VALKUIL MM
        1 BLK FORM 0.1 Z X+0 Y+0 Z-20
        2 BLK FORM 0.2 X+100 Y+100 Z+0
        3 TOOL CALL "FREES16" Z S3500 F1000
        4 L Z+50 R0 FMAX
        5 L X-15 Y-15 R0 FMAX
        6 L Z-5 R0 F400 M3
        7 APPR LCT X+10 Y+10 R10 RL F800
        8 L X+90 Y+10
        9 L X+90 Y+90
        10 L X+10 Y+90 R0 ; <-- FOUT: R0 ingevoerd via ENT i.p.v. NO ENT! Schakelt RL uit!
        11 L X+10 Y+10
        12 DEP LCT X-15 Y-15 R10 F800
        13 L Z+100 R0 FMAX M30
        14 END PGM FOUT_R0_VALKUIL MM
    """.trimIndent()

    /**
     * Foutdemo 2: "Gereedschapsradius te groot" (FREES25 in binnenhoek met RND R8).
     */
    val demoRadiusTooLarge = """
        0 BEGIN PGM FOUT_RADIUS_TE_GROOT MM
        1 BLK FORM 0.1 Z X+0 Y+0 Z-20
        2 BLK FORM 0.2 X+100 Y+100 Z+0
        3 ; FREES25 heeft R=12.5 mm. Dit past fysiek NOOIT in een binnenhoek RND R8!
        4 TOOL CALL "FREES25" Z S2800 F1000
        5 L Z+50 R0 FMAX
        6 L X-20 Y-20 R0 FMAX
        7 L Z-5 R0 F400 M3
        8 APPR LCT X+20 Y+20 R15 RL F800
        9 L X+80 Y+20
        10 RND R8 ; <-- FOUT: TNC 640 stopt: Gereedschapsradius te groot (Reff=12.5 > RND R8)
        11 L X+80 Y+80
        12 DEP LCT X-20 Y-20 R15 F800
        13 L Z+100 R0 FMAX M30
        14 END PGM FOUT_RADIUS_TE_GROOT MM
    """.trimIndent()

    /**
     * Foutdemo 3: "Te grote diameter boring" bij Cyclus 208 Boorfrezen (Q342=0 en Q335=30 bij D=6 mm).
     */
    val demoCycle208TooLarge = """
        0 BEGIN PGM FOUT_CYCLUS208 MM
        1 BLK FORM 0.1 Z X+0 Y+0 Z-20
        2 BLK FORM 0.2 X+100 Y+100 Z+0
        3 TOOL CALL "KANTENFREES6" Z S4000 F600 DR-1.5
        4 ; D_eff = 2 * (3.0 - 1.5) = 3.0 mm. 2 x D_eff = 6.0 mm.
        5 ; Q335=25 mm met Q342=0 mm overschrijdt 2 x D_eff zwaar!
        6 CYCL DEF 208 BOORFREZEN ~
           Q200=+2 ;VEILIGHEIDSAFST. ~
           Q201=-5 ;DIEPTE ~
           Q206=+300 ;AANZETVOEDING DIEPTE ~
           Q334=+1 ;AANZETDIEPTE ~
           Q203=+0 ;COORD. OPPERVLAK ~
           Q204=+50 ;2E VEILIGHEIDSAFST. ~
           Q335=+25 ;NOMINALE DIAMETER ~
           Q342=+0 ;VOORBOORDIAMETER = 0! (FOUT) ~
           Q351=+1 ;FREESWIJZE MEELOPEND
        7 L X+50 Y+50 R0 FMAX M3
        8 CYCL CALL
        9 L Z+100 R0 FMAX M30
        10 END PGM FOUT_CYCLUS208 MM
    """.trimIndent()

    val all: List<Pair<String, String>> = listOf(
        "CONTOURFREZEN_V4.H (Omtrek & Ontbramen)" to contourfrezenV4,
        "BOOR_FREESCYCLI.H (Vlakken, Kamers, Sleuf)" to boorFreescycli,
        "KAMERSFREZEN_H5.H (Kamers & 3x M8 Tappen)" to kamersfrezenH5,
        "U_VORM_LABEL.H (Nulpunt Cyclus 7 & Boorfrees 208)" to uVormLabel,
        "FOUTDEMO: R0 vs. RL Valkuil" to demoR0Pitfall,
        "FOUTDEMO: Gereedschapsradius te groot" to demoRadiusTooLarge,
        "FOUTDEMO: Cyclus 208 Te grote diameter" to demoCycle208TooLarge
    )
}
