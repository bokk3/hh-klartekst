# HEIDENHAIN Opleidingscurriculum & Praktijkoefeningen

Dit document biedt een uitgebreide didactische en technische toelichting op de vijf officiële HEIDENHAIN praktijkopdrachten die zijn geïntegreerd in de simulator (gebaseerd op het lesmateriaal uit [github.com/bokk3/heidenhain](https://github.com/bokk3/heidenhain)).

Elk hoofdstuk bevat:
1. De didactische doelstelling (waarom deze oefening in de opleiding zit).
2. De volledige, geteste Klartext (`.H`) code.
3. Wiskundige achtergrond en CNC-strategie.
4. Uitleg van HEIDENHAIN-specifieke commando's en valkuilen.
5. Typische examenvragen en antwoorden voor docenten/beoordelaars.

---

## Inhoudsopgave
- [Oefening 1: Regelmatige Vijfhoek (Tekening 1226651)](#oefening-1-regelmatige-vijfhoek-tekening-1226651)
- [Oefening 2: Ruitvormig Opstaand Eiland (Tekening 1206129)](#oefening-2-ruitvormig-opstaand-eiland-tekening-1206129)
- [Oefening 3: Boogcontour & Ronde Kamer (Tekening 1214098)](#oefening-3-boogcontour--ronde-kamer-tekening-1214098)
- [Oefening 4: Vaste Bewerkingscycli 251, 252, 253 (Tekening 1214155)](#oefening-4-vaste-bewerkingscycli-251-252-253-tekening-1214155)
- [Oefening 5: 22 Gaten Boren & Tappen met Subprogramma's (Tekening 1226642)](#oefening-5-22-gaten-boren--tappen-met-subprogrammas-tekening-1226642)

---

## Oefening 1: Regelmatige Vijfhoek (Tekening 1226651)

### Didactisch doel
Het aanleren van **polaire coördinaten** (`CC` en `LP`) en het vermijden van tijdrovend handmatig rekenwerk (sinus en cosinus) bij schuine contouren. Daarnaast introduceert deze oefening de zogenaamde *"Pool-verplaatsingstruc"* (een lege `CC`).

### Klartext Code
```klartext
0 BEGIN PGM 1226651 MM
1 BLK FORM 0.1 Z X+0 Y+0 Z-16
2 BLK FORM 0.2 X+100 Y+100 Z+0
3 TOOL CALL 5 Z S8900 F1100
4 L Z+100 R0 FMAX M3
5 L X+50 Y+20 R0 FMAX
6 L Z+5 R0 FMAX M8
7 L Z-5 R0 F1100
8 L X+30 Y+20 R0 F1100
9 CC
10 LP PR+40 PA+108 R0
11 CC
12 LP PR+40 PA+36 R0
13 CC
14 LP PR+40 PA-36 R0
15 CC
16 LP PR+40 PA-108 R0
17 L X+50 Y+20 R0
18 L Z+5 R0 F2000 M9
19 L X+150 Y+150 Z+100 R0 FMAX M30
20 END PGM 1226651 MM
```

### Wiskundige Analyse & CNC-Strategie
1. **Waarom frezen met `R0` (ongecorrigeerd)?**
   - Op de werktekening staat de maatvoering exact op het hart van een $10\text{ mm}$ brede sleuf.
   - We gebruiken gereedschap **T5** (vingerfrees $\varnothing 10\text{ mm}$).
   - Omdat freesdiameter en sleufbreedte identiek zijn, loopt het hart van het gereedschap samen met de hartlijn van de tekening. Radiuscompensatie (`RL` of `RR`) is daarom niet nodig.

2. **Waarom polaire coördinaten (`CC` + `LP`) in plaats van Cartesiaans (`X`/`Y`)?**
   - Een regelmatige vijfhoek heeft een constante zijdelengte van $40\text{ mm}$ (van $X+30$ tot $X+70$ op $Y+20$).
   - De binnenhoek van een regelmatige vijfhoek is altijd:
     $$\text{Binnenhoek} = \frac{(5 - 2) \times 180^\circ}{5} = 108^\circ$$
   - Als je in $X/Y$ programmeert, moet je per hoekpunt goniometrische projecties uitrekenen. Met `LP PR+40 PA` programmeer je direct de lengte ($40\text{ mm}$) en de hoek.

3. **De "Pool-verplaatsingstruc" (lege `CC`):**
   - Normaal gesproken definieert `CC X... Y...` een vast middelpunt.
   - Programmeer je een `CC` **zonder coördinaten**, dan legt HEIDENHAIN de nieuwe pool exact op de huidige positie van het gereedschap.
   - Omdat we met de klok mee frezen, draait de frees bij elke hoek $180^\circ - 108^\circ = 72^\circ$ naar rechts. Vandaar dat de polaire hoeken telkens met $72^\circ$ afnemen:
     $$+108^\circ \to +36^\circ \to -36^\circ \to -108^\circ$$

---

## Oefening 2: Ruitvormig Opstaand Eiland (Tekening 1206129)

### Didactisch doel
Het beheersen van gereedschapsradiuscompensatie (`RL`), meelopend frezen, en het toepassen van vloeiende aanloop- en uitloopfuncties (`APPR LT` en `DEP LT`).

### Klartext Code
```klartext
0 BEGIN PGM 1206129 MM
1 BLK FORM 0.1 Z X+0 Y+0 Z-20
2 BLK FORM 0.2 X+100 Y+100 Z+0
3 TOOL CALL 10 Z S4500 F1100
4 L Z+100 R0 FMAX M3
5 L X-20 Y+20 R0 FMAX
6 L Z+5 R0 FMAX M8
7 L Z-5 R0 F1100
8 APPR LT X+0 Y+25 LEN20 RL F1100
9 L X+25 Y+100
10 L X+100 Y+75
11 L X+75 Y+0
12 L X+0 Y+25
13 DEP LT LEN20 F2000
14 L Z+5 R0 F2000 M9
15 L X+150 Y+150 Z+100 R0 FMAX M30
16 END PGM 1206129 MM
```

### Wiskundige Analyse & CNC-Strategie
1. **Waarom radiuscorrectie `RL`?**
   - Het werkstuk is een ruitvormig eiland (buitencontour). Het materiaal dat moet blijven staan bevindt zich aan de rechterkant van onze beweging.
   - De frees loopt links van de contourlijn in de voorwaartse richting: **`RL` (Radius Left)**. Dit garandeert meelopend frezen (climb milling), wat zorgt voor een gladde oppervlaktekwaliteit en langere standtijd van de wisselplaten.

2. **Werking van `APPR LT` (Approach Line Tangential):**
   - Rechtstreeks insteken op de contour veroorzaakt een lelijke aanzetmarkering (deukje).
   - `APPR LT X+0 Y+25 LEN20 RL` berekent automatisch een hulppunt $P_H$ dat precies $20\text{ mm}$ in het verlengde vóór de eerste schuine lijn ligt:
     $$P_H = P_{\text{start}} - t_{\text{contour}} \times \text{LEN}$$
   - De frees activeert de radiuscompensatie in het luchtledige en glijdt naadloos de eerste schuine zijde in.
   - **Gouden regel:** $\text{LEN}$ moet altijd groter zijn dan de gereedschapsradius ($R = 10\text{ mm}$, dus $\text{LEN} = 20\text{ mm}$ is veilig).

3. **Werking van `DEP LT` (Departure Line Tangential):**
   - Na het sluiten van de contour bij `X+0 Y+25` loopt `DEP LT LEN20` nog $20\text{ mm}$ tangentieel door in het verlengde van de laatste zijde.
   - Tegelijkertijd schakelt de besturing de radiuscompensatie automatisch weer uit naar `R0`.

---

## Oefening 3: Boogcontour & Ronde Kamer (Tekening 1214098)

### Didactisch doel
Het programmeren van cirkelbogen met een expliciet cirkelmiddelpunt (`CC`) en de cirkelopdracht `C` met draairichting (`DR+`/`DR-`). Tevens inzicht krijgen in het geometrische effect van de freesradius op inwendige bochten.

### Klartext Code
```klartext
0 BEGIN PGM 1214098 MM
1 BLK FORM 0.1 Z X+0 Y+0 Z-16
2 BLK FORM 0.2 X+100 Y+100 Z+0
3 TOOL CALL 8 Z S5600 F1600
4 L Z+100 R0 FMAX M3
5 L X-20 Y-20 R0 FMAX
6 L Z+5 R0 FMAX M8
7 L Z-5 R0 F1600
8 APPR LT X+10 Y+0 LEN20 RL F1600
9 L Y+14
10 CC X+25 Y+25
11 C X+10 Y+36 DR+
12 L Y+100
13 DEP LT LEN20 F2000
14 L Z+5 R0 F2000 M9
15 L X+150 Y+150 Z+100 R0 FMAX M30
16 END PGM 1214098 MM
```

### Wiskundige Analyse & CNC-Strategie
1. **Berekening van de boogstraal via Pythagoras:**
   - Cirkelmiddelpunt ligt op $(25, 25)$.
   - Beginpunt van de cirkel ligt op $(10, 14)$.
   - De werkelijke boogstraal $R_{\text{boog}}$ is:
     $$R_{\text{boog}} = \sqrt{(25 - 10)^2 + (25 - 14)^2} = \sqrt{15^2 + 11^2} = \sqrt{225 + 121} = \sqrt{346} \approx 18.60\text{ mm}$$
   - Door `CC X+25 Y+25` en `C X+10 Y+36 DR+` te gebruiken, berekent de HEIDENHAIN-besturing deze straal exact zelf.

2. **Waarom blijft er in een zuivere contourbaan een klein eilandje staan?**
   - De gebruikte frees is **T8** ($\varnothing 16\text{ mm}$, radius $R = 8\text{ mm}$).
   - Tijdens het doorlopen van de boog met radiuscompensatie `RL` loopt de binnensnijkant van de frees op afstand:
     $$D_{\text{binnen}} = R_{\text{boog}} - 2 \times R_{\text{frees}} = 18.60 - 16.0 = 2.60\text{ mm}$$
   - Er blijft in het hart van de boog dus een klein pilaartje van $\varnothing 5.2\text{ mm}$ staan. Dit is geen programmeerfout, maar illustreert het verschil tussen een **contourbaan** en een **kameruitruimcyclus**.

3. **Draairichting `DR+`:**
   - In HEIDENHAIN Klartext staat `DR+` altijd voor een draaiing **tegen de klok in** (Counter-Clockwise).
   - Vanaf $(10, 14)$ via de rechterboog naar $(10, 36)$ is een linksdraaiende cirkelboog.

---

## Oefening 4: Vaste Bewerkingscycli 251, 252, 253 (Tekening 1214155)

### Didactisch doel
Het toepassen van standaard bewerkingscycli voor kamers en slobgaten. Het begrijpen van de modale cyclusdefinitie (`CYCL DEF`) en de positieaanroep via `M99`.

### Klartext Code
```klartext
0 BEGIN PGM 1214155 MM
1 BLK FORM 0.1 Z X+0 Y+0 Z-16
2 BLK FORM 0.2 X+100 Y+100 Z+0
3 TOOL CALL 5 Z S8900 F1100
4 L Z+100 R0 FMAX M3
5 L X+150 Y+150 R0 FMAX
6 CYCL DEF 251 RECHTHOEKIGE KAMER ~
    Q215=+0    ;BEWERKINGSOMVANG ~
    Q218=+70   ;1E ZIJDELENGTE ~
    Q219=+20   ;2E ZIJDELENGTE ~
    Q220=+6    ;HOEKRADIUS ~
    Q368=+0.2  ;OVERMAAT ZIJKANT ~
    Q224=+90   ;ROTATIEPOSITIE ~
    Q367=+0    ;KAMERPOSITIE ~
    Q207=+1100 ;AANZET FREZEN ~
    Q351=+1    ;FREESWIJZE ~
    Q201=-5    ;DIEPTE ~
    Q202=+5    ;DIEPTEVERPLAATSING ~
    Q369=+0.1  ;OVERMAAT DIEPTE ~
    Q206=+1100 ;AANZET DIEPTEVERPL. ~
    Q338=+5    ;VERPLAATSING NABEW. ~
    Q200=+5    ;VEILIGHEIDSAFSTAND ~
    Q203=+0    ;COORD. OPPERVLAK ~
    Q204=+50   ;2E VEILIGHEIDSAFST. ~
    Q370=+0.7  ;BAANOVERLAPPING ~
    Q366=+1    ;INSTEKEN ~
    Q385=+1100 ;AANZET NABEWERKEN ~
    Q439=+0    ;REF. AANZET
7 L X+30 Y+50 R0 FMAX M8 M99
8 CYCL DEF 252 RONDE KAMER ~
    Q215=+0    ;BEWERKINGSOMVANG ~
    Q223=+30   ;CIRKELDIAMETER ~
    Q368=+0.2  ;OVERMAAT ZIJKANT ~
    Q207=+1100 ;AANZET FREZEN ~
    Q351=+1    ;FREESWIJZE ~
    Q201=-5    ;DIEPTE ~
    Q202=+5    ;DIEPTEVERPLAATSING ~
    Q369=+0.1  ;OVERMAAT DIEPTE ~
    Q206=+1100 ;AANZET DIEPTEVERPL. ~
    Q338=+5    ;VERPLAATSING NABEW. ~
    Q200=+5    ;VEILIGHEIDSAFSTAND ~
    Q203=+0    ;COORD. OPPERVLAK ~
    Q204=+50   ;2E VEILIGHEIDSAFST. ~
    Q370=+0.7  ;BAANOVERLAPPING ~
    Q366=+1    ;INSTEKEN ~
    Q385=+1100 ;AANZET NABEWERKEN ~
    Q439=+0    ;REF. AANZET
9 L X+75 Y+60 R0 FMAX M99
10 CYCL DEF 253 SLEUFFREZEN ~
    Q215=+0    ;BEWERKINGSOMVANG ~
    Q218=+50   ;SLEUFLENGTE ~
    Q219=+12   ;SLEUFBREEDTE ~
    Q368=+0.2  ;OVERMAAT ZIJKANT ~
    Q374=+20   ;ROTATIEPOSITIE ~
    Q367=+0    ;SLEUF POSITIE ~
    Q207=+1100 ;AANZET FREZEN ~
    Q351=+1    ;FREESWIJZE ~
    Q201=-5    ;DIEPTE ~
    Q202=+5    ;DIEPTEVERPLAATSING ~
    Q369=+0.1  ;OVERMAAT DIEPTE ~
    Q206=+1100 ;AANZET DIEPTEVERPL. ~
    Q338=+5    ;VERPLAATSING NABEW. ~
    Q200=+5    ;VEILIGHEIDSAFSTAND ~
    Q203=+0    ;COORD. OPPERVLAK ~
    Q204=+50   ;2E VEILIGHEIDSAFST. ~
    Q366=+2    ;INSTEKEN ~
    Q385=+1100 ;AANZET NABEWERKEN ~
    Q439=+0    ;REF. AANZET
11 L X+70 Y+22 R0 FMAX M99
12 L Z+100 R0 FMAX M9
13 L X+150 Y+150 R0 FMAX M30
14 END PGM 1214155 MM
```

### Vergelijking van de 3 Cycli
| Eigenschap | Cyclus 251 (Rechthoekig) | Cyclus 252 (Rond) | Cyclus 253 (Sleuf) |
| :--- | :--- | :--- | :--- |
| **Vorm** | $70 \times 20\text{ mm}$ met $R6$ | $\varnothing 30\text{ mm}$ cirkel | $50 \times 12\text{ mm}$ halfrond |
| **Insteken (`Q366`)** | Helicaal / Spiraalvormig (`+1`) | Helicaal / Spiraalvormig (`+1`) | Pendelend (`+2`) |
| **Rotatie** | `Q224=+90` ($90^\circ$ gedraaid) | Niet van toepassing | `Q374=+20` ($20^\circ$ gedraaid) |
| **Aanroep** | `L X+30 Y+50 ... M99` | `L X+75 Y+60 ... M99` | `L X+70 Y+22 ... M99` |

---

## Oefening 5: 22 Gaten Boren & Tappen met Subprogramma's (Tekening 1226642)

### Didactisch doel
Het structureren van complexe werkstukken met meerdere bewerkingsstappen (voorboren en tappen) en meerdere gereedschappen via subprogramma's (`LBL 10..12`) en programmadeelherhalingen (`CALL LBL REP`).

### Klartext Code
```klartext
0 BEGIN PGM 1226642 MM
1 BLK FORM 0.1 Z X+0 Y+0 Z-16
2 BLK FORM 0.2 X+100 Y+100 Z+0
3 * - VOORBOREN Ø6.8 (T229)
4 TOOL CALL 229 Z S6000
5 L Z+100 R0 FMAX M3 M8
6 CYCL DEF 200 BOREN ~
    Q200=+5    ;VEILIGHEIDSAFSTAND ~
    Q201=-17   ;DIEPTE ~
    Q206=+840  ;AANZET DIEPTEVERPL. ~
    Q202=+17   ;DIEPTEVERPLAATSING ~
    Q210=+0    ;STILSTANDSTIJD BOVEN ~
    Q203=+0    ;COORD. OPPERVLAK ~
    Q204=+50   ;2E VEILIGHEIDSAFST. ~
    Q211=+0    ;STILSTANDSTIJD ONDER ~
    Q395=+0    ;REF. DIEPTE
7 CALL LBL 10
8 CALL LBL 11
9 CALL LBL 12
10 * - TAPPEN M8 (T264)
11 L Z+100 R0 FMAX M5 M9
12 L X+150 Y+150 R0 FMAX
13 TOOL CALL 264 Z S200
14 L Z+100 R0 FMAX M3 M8
15 CYCL DEF 206 SCHROEFDRAAD TAPPEN ~
    Q200=+5    ;VEILIGHEIDSAFSTAND ~
    Q201=-17   ;SCHROEFDRAADDIEPTE ~
    Q206=+250  ;AANZET (200 x 1.25) ~
    Q211=+0    ;STILSTANDSTIJD ONDER ~
    Q203=+0    ;COORD. OPPERVLAK ~
    Q204=+50   ;2E VEILIGHEIDSAFST.
16 CALL LBL 10
17 CALL LBL 11
18 CALL LBL 12
19 L Z+100 R0 FMAX M5 M9
20 L X+150 Y+150 R0 FMAX
21 M30
22 * - SUBPROGRAMMA'S
23 LBL 10
24 * - RIJ 1 (8 GATEN)
25 L X+30 Y+15 R0 FMAX
26 LBL 1
27 CYCL CALL
28 L IX+3.420 IY+9.397 FMAX
29 CALL LBL 1 REP 7
30 LBL 0
31 LBL 11
32 * - RIJ 2 (8 GATEN)
33 L X+11.206 Y+21.840 R0 FMAX
34 LBL 2
35 CYCL CALL
36 L IX+3.420 IY+9.397 FMAX
37 CALL LBL 2 REP 7
38 LBL 0
39 LBL 12
40 * - CIRKEL 6x60 R15
41 CC X+70 Y+30
42 LP PR+15 PA+30 R0 FMAX
43 LBL 3
44 CYCL CALL
45 LP IPA+60 FMAX
46 CALL LBL 3 REP 5
47 LBL 0
48 END PGM 1226642 MM
```

### Wiskundige Analyse & CNC-Strategie
1. **Synchronisatie van Toerental en Spoed bij Tappen:**
   - Schroefdraad is **M8 standaard metrisch**. De spoed $P = 1.25\text{ mm/omw}$.
   - Bij een spiltoerental van $S = 200\text{ omw/min}$ geldt:
     $$F = S \times P = 200 \times 1.25 = 250\text{ mm/min}$$
   - Deze voeding (`Q206=+250`) is essentieel om te voorkomen dat de tap breekt of de draad kapot getrokken wordt.

2. **Drie gatenpatronen:**
   - **Rij 1 (`LBL 10`):** Start op $(30, 15)$. Verplaatst 7 keer incrementeel met `IX+3.420 IY+9.397` $\to$ 8 gaten.
   - **Rij 2 (`LBL 11`):** Start op $(11.206, 21.840)$. Verplaatst 7 keer incrementeel met `IX+3.420 IY+9.397` $\to$ 8 gaten.
   - **Steekcirkel (`LBL 12`):** Middelpunt op $(70, 30)$, straal $15\text{ mm}$. Start op poolhoek $30^\circ$. Draait 5 keer met incrementele hoek `LP IPA+60` $\to$ 6 gaten ($360^\circ / 60^\circ = 6$).
   - **Totaal:** $8 + 8 + 6 = 22\text{ gaten}$. Omdat beide gereedschappen (boor en tap) exact dezelfde labels aanroepen, worden er in totaal $44$ cyclusbewerkingen foutloos uitgevoerd!

3. **Verschil tussen `CALL LBL` en `CALL LBL REP`:**
   - `CALL LBL 10`: Roept een subprogramma aan dat eindigt met `LBL 0`.
   - `CALL LBL 1 REP 7`: Een **programmadeelherhaling** (lus). Springt 7 keer terug naar `LBL 1` en loopt daarna gewoon door naar de volgende regel. Heeft **geen** `LBL 0` nodig!
