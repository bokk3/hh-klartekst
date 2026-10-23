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

    /**
     * 8. PARAMETRISCH_ELLIPS_Q.H:
     * Volledig parametrisch geprogrammeerde ellips via TNC 640 Q-parameters:
     * - Q1 = Halve lange as A (50 mm)
     * - Q2 = Halve korte as B (30 mm)
     * - Q3 = Start-hoek phi in graden (0..360)
     * - Q4 = Stapgrootte d_phi (5 graden)
     * - FN 6 (SIN), FN 7 (COS), FN 3 (MULTIPLY), FN 1 (ADD)
     * - FN 12: IF +Q3 LT +365 GOTO LBL "ELLIPSLUS"
     */
    val demoParametricEllipse = """
        0 BEGIN PGM PARAMETRISCH_ELLIPS_Q MM
        1 ; --- RUWDEEL DEFINITIE ---
        2 BLK FORM 0.1 Z X-75 Y-75 Z-20
        3 BLK FORM 0.2 X+75 Y+75 Z+0
        4 ; ===================================================
        5 ; PARAMETRISCHE ELLIPS VIA Q-PARAMETERS & FN 6/7/12
        6 ; Q1 = Halve lange as A (50 mm)
        7 ; Q2 = Halve korte as B (30 mm)
        8 ; Q3 = Start-hoek phi in graden (0..360)
        9 ; Q4 = Stapgrootte d_phi (5 graden)
        10 ; ===================================================
        11 FN 0: Q1 = +50
        12 FN 0: Q2 = +30
        13 FN 0: Q3 = +0
        14 FN 0: Q4 = +5
        15 TOOL CALL "FREES10" Z S3500 F800
        16 L Z+50 R0 FMAX
        17 ; Bereken startpositie bij hoek 0: X = A * COS(0) = 50, Y = B * SIN(0) = 0
        18 FN 7: Q10 = COS +Q3
        19 FN 3: Q10 = +Q10 * +Q1
        20 FN 6: Q11 = SIN +Q3
        21 FN 3: Q11 = +Q11 * +Q2
        22 L X+Q10 Y+Q11 R0 FMAX
        23 L Z-5 R0 F400 M3
        24 ; --- ITERATIEVE LUS VOOR ELLIPS PUNTEN ---
        25 LBL "ELLIPSLUS"
        26 FN 7: Q10 = COS +Q3
        27 FN 3: Q10 = +Q10 * +Q1
        28 FN 6: Q11 = SIN +Q3
        29 FN 3: Q11 = +Q11 * +Q2
        30 L X+Q10 Y+Q11 R0 F800
        31 FN 1: Q3 = +Q3 + +Q4
        32 FN 12: IF +Q3 LT +365 GOTO LBL "ELLIPSLUS"
        33 L Z+50 R0 FMAX M30
        34 END PGM PARAMETRISCH_ELLIPS_Q MM
    """.trimIndent()

    val slCycliEilanden = """
        0 BEGIN PGM SL_CYCLI_EILANDEN MM
        1 ; ===================================================
        2 ; PIJLER 1 DEMO: SL-CYCLI MET EILAND (CYCL 14, 20, 22, 24)
        3 ; VAKSTUDIE: DIEPE RECHTHOEKIGE KAMER MET CENTRAAL EILAND
        4 ; ===================================================
        5 BLK FORM 0.1 Z X-50 Y-50 Z-25
        6 BLK FORM 0.2 X+50 Y+50 Z+0
        7 TOOL CALL "FREES10" Z S3200 F600
        8 ; --- SL-CONTOUR LIJST DEFINITIE (1 = KAMER, 2 = EILAND) ---
        9 CYCL DEF 14.0 KONTUR
        10 CYCL DEF 14.1 KONTURLABEL 1 / 2
        11 ; --- CYCLUS 20: ALGEMENE CONTOURGEGEVENS ---
        12 CYCL DEF 20 KONTUR-DATEN ~
             Q1=-12 ;FRAES-TIEFE ~
             Q2=+1 ;BAAN-OVERLAPPING ~
             Q3=+0.4 ;OVERMAAT ZIJKANT ~
             Q4=+0.1 ;OVERMAAT DIEPTE ~
             Q5=+0 ;COOR. OPPERVLAK ~
             Q6=+2 ;VEILIGHEIDSAFST. ~
             Q7=+50 ;VEILIGE HOOGTE ~
             Q8=+0 ;AFRONDINGSRADIUS ~
             Q9=+1 ;DRAAIRICHTING
        13 ; --- CYCLUS 22: RUWFREZEN MET VERMIJDING VAN HET EILAND ---
        14 CYCL DEF 22 AUSRAEUMEN ~
             Q10=+6 ;INSTEEKDIEPTE ~
             Q11=+180 ;VOEDING DIEPTEVERPL. ~
             Q12=+550 ;VOEDING VOORBEWERKEN ~
             Q18=+0 ;VOORRUW-WERKTUIG ~
             Q19=+100 ;VOEDING PENDELEN ~
             Q208=+99999 ;VOEDING TERUGTREKKEN
        15 CYCL CALL
        16 ; --- CYCLUS 24: NAFREZEN ZIJKANTEN (KAMERWAND & EILANDWAND) ---
        17 CYCL DEF 24 SCHLICHTEN SEITE ~
             Q9=+1 ;DRAAIRICHTING ~
             Q10=+6 ;INSTEEKDIEPTE ~
             Q11=+180 ;VOEDING DIEPTEVERPL. ~
             Q12=+450 ;VOEDING VOORBEWERKEN ~
             Q14=+0 ;OVERMAAT ZIJKANT
        18 CYCL CALL
        19 L Z+50 R0 FMAX M30
        20 ; ===================================================
        21 ; SUB-CONTOUR 1: BUITENKAMER 80x80 MM
        22 ; ===================================================
        23 LBL 1
        24 L X-40 Y-40 RR
        25 L X+40 Y-40
        26 L X+40 Y+40
        27 L X-40 Y+40
        28 L X-40 Y-40
        29 LBL 0
        30 ; ===================================================
        31 ; SUB-CONTOUR 2: CENTRAAL EILAND 30x30 MM
        32 ; ===================================================
        33 LBL 2
        34 L X-15 Y-15 RL
        35 L X+15 Y-15
        36 L X+15 Y+15
        37 L X-15 Y+15
        38 L X-15 Y-15
        39 LBL 0
        40 END PGM SL_CYCLI_EILANDEN MM
    """.trimIndent()

    val steekcirkelVlakfrees = """
        0 BEGIN PGM STEEKCIRKEL_VLAKFREES MM
        1 ; ===================================================
        2 ; PIJLER 1 DEMO: VLAKFREZEN (232), STEEKCIRKEL (220),
        3 ; BOREN (200), ROTATIE (10) & SPIEGELING (8)
        4 ; ===================================================
        5 BLK FORM 0.1 Z X-60 Y-60 Z-25
        6 BLK FORM 0.2 X+60 Y+60 Z+0
        7 ; --- 1. VLAKFREZEN VAN DE BOVENKANT MET VLAKFREES63 ---
        8 TOOL CALL "VLAKFREES63" Z S2200 F900
        9 CYCL DEF 232 PLANFRAESEN ~
             Q389=+0 ;STRATEGIE (MEANDER) ~
             Q225=-55 ;STARTPUNT 1E AS ~
             Q226=-55 ;STARTPUNT 2E AS ~
             Q227=+0 ;STARTPUNT 3E AS ~
             Q386=-1.5 ;EINDPUNT 3E AS ~
             Q218=+110 ;1E ZIJDENLENGTE ~
             Q219=+110 ;2E ZIJDENLENGTE ~
             Q202=+1.5 ;MAX. INSTEEKDIEPTE ~
             Q369=+0 ;OVERMAAT BODEM ~
             Q370=+1.2 ;OVERLAPPING ~
             Q207=+800 ;VOEDING FREZEN ~
             Q385=+600 ;VOEDING NASNIJDEN ~
             Q253=+99999 ;VOEDING VOORPOS. ~
             Q200=+2 ;VEILIGHEIDSAFST. ~
             Q204=+50 ;2E VEILIGHEIDSAFST.
        10 CYCL CALL
        11 ; --- 2. CENTERBOOR OP STEEKCIRKEL MET 6 GATEN ---
        12 TOOL CALL "CENTERBOOR" Z S3000 F250
        13 CYCL DEF 240 CENTREREN ~
             Q200=+2 ;VEILIGHEIDSAFST. ~
             Q201=-3.5 ;DIEPTE ~
             Q206=+200 ;VOEDING DIEPTEVERPL. ~
             Q211=+0 ;STILSTANDTIJD ~
             Q203=+0 ;COOR. OPPERVLAK ~
             Q204=+50 ;2E VEILIGHEIDSAFST.
        14 CYCL DEF 220 PUNKTEMUSTER KREIS ~
             Q216=+0 ;MIDDEN 1E AS ~
             Q217=+0 ;MIDDEN 2E AS ~
             Q244=+70 ;STEEKCIRKELDIAMETER ~
             Q245=+0 ;STARTHOEK ~
             Q246=+360 ;STOPHOEK ~
             Q247=+60 ;HOEKSTAP ~
             Q241=+6 ;AANTAL BEWERKINGEN ~
             Q200=+2 ;VEILIGHEIDSAFST. ~
             Q203=+0 ;COOR. OPPERVLAK ~
             Q204=+50 ;2E VEILIGHEIDSAFST. ~
             Q301=+1 ;NAAR VEILIGE HOOGTE
        15 CYCL CALL
        16 ; --- 3. BOREN MET BOOR6 OP DEZELFDE STEEKCIRKEL ---
        17 TOOL CALL "BOOR6" Z S2400 F180
        18 CYCL DEF 200 BOHREN ~
             Q200=+2 ;VEILIGHEIDSAFST. ~
             Q201=-18 ;DIEPTE ~
             Q206=+150 ;VOEDING DIEPTEVERPL. ~
             Q202=+6 ;INSTEEKDIEPTE ~
             Q210=+0 ;STILSTANDTIJD BOVEN ~
             Q203=+0 ;COOR. OPPERVLAK ~
             Q204=+50 ;2E VEILIGHEIDSAFST. ~
             Q211=+0.1 ;STILSTANDTIJD ONDER
        19 CYCL CALL
        20 L Z+100 R0 FMAX M30
        21 END PGM STEEKCIRKEL_VLAKFREES MM
    """.trimIndent()

    val all: List<Pair<String, String>> = listOf(
        "CONTOURFREZEN_V4.H (Omtrek & Ontbramen)" to contourfrezenV4,
        "SL_CYCLI_EILANDEN.H (SL 14, 20, 22, 24 met Eilanden)" to slCycliEilanden,
        "STEEKCIRKEL_VLAKFREES.H (Vlakken 232 & Steekcirkel 220)" to steekcirkelVlakfrees,
        "BOOR_FREESCYCLI.H (Kamers & Sleuf)" to boorFreescycli,
        "KAMERSFREZEN_H5.H (Kamers & 3x M8 Tappen)" to kamersfrezenH5,
        "U_VORM_LABEL.H (Nulpunt Cyclus 7 & Boorfrees 208)" to uVormLabel,
        "PARAMETRISCH_ELLIPS_Q.H (Q-Parameters & FN 12 Lus)" to demoParametricEllipse,
        "FOUTDEMO: R0 vs. RL Valkuil" to demoR0Pitfall,
        "FOUTDEMO: Gereedschapsradius te groot" to demoRadiusTooLarge,
        "FOUTDEMO: Cyclus 208 Te grote diameter" to demoCycle208TooLarge
    )
}
