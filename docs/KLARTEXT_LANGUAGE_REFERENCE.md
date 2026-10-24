# HEIDENHAIN Klartext Taalreferentie & Syntaxhandleiding

Deze handleiding beschrijft de volledige syntaxis en het semantische gedrag van de dialooggestuurde programmeertaal **HEIDENHAIN Klartext** (`.H`), zoals geïmplementeerd in deze Android CNC Simulator.

---

## Inhoudsopgave
1. [Programmastructuur & Header](#1-programmastructuur--header)
2. [Ruwdeeldefinitie (BLK FORM)](#2-ruwdeeldefinitie-blk-form)
3. [Gereedschappen (TOOL DEF & TOOL CALL)](#3-gereedschappen-tool-def--tool-call)
4. [Baanbewegingen (Cartesiaans & Polair)](#4-baanbewegingen-cartesiaans--polair)
5. [Contour Aanrijden & Verlaten (APPR & DEP)](#5-contour-aanrijden--verlaten-appr--dep)
6. [Hoekafrondingen & Afschuiningen (RND & CHF)](#6-hoekafrondingen--afschuiningen-rnd--chf)
7. [Subprogramma's & Herhalingen (LBL)](#7-subprogrammas--herhalingen-lbl)
8. [Vaste Bewerkingscycli (Canned Cycles)](#8-vaste-bewerkingscycli-canned-cycles)
9. [SL-Cycli (Subcontourlijsten)](#9-sl-cycli-subcontourlijsten)
10. [Coördinatentransformaties (Cycli 7, 8, 10)](#10-coördinatentransformaties-cycli-7-8-10)
11. [Q-Parameters & Rekenfuncties (FN)](#11-q-parameters--rekenfuncties-fn)
12. [M-Hulpfuncties](#12-m-hulpfuncties)

---

## 1. Programmastructuur & Header

Elk Klartext-programma start met `BEGIN PGM` en eindigt verplicht met `END PGM`.

```klartext
0 BEGIN PGM PROFIEL MM
...
25 END PGM PROFIEL MM
```

- **Eenheid**: `MM` (millimeter) of `INCH`. De simulator rekent intern standaard in millimeters ($1\text{ inch} = 25.4\text{ mm}$).
- **Regelnummers**: Oplopend van $0$. Als regels worden ingevoegd of verwijderd, hernummert de TNC-besturing de blokken automatisch.

---

## 2. Ruwdeeldefinitie (BLK FORM)

Om de 3D-materiaalsimulatie en de grafische weergave te initialiseren, worden twee `BLK FORM` regels gebruikt:

```klartext
1 BLK FORM 0.1 Z X+0 Y+0 Z-20
2 BLK FORM 0.2 X+100 Y+80 Z+0
```

- **`BLK FORM 0.1 [Spilas] X... Y... Z...`**: Het *MIN-punt* van het ruwdeel (linkeronderhoek op de bodem).
- **`BLK FORM 0.2 X... Y... Z...`**: Het *MAX-punt* van het ruwdeel (rechterbovenhoek aan het bovenoppervlak $Z0$).
- **Spilas**: Meestal `Z` voor verticale 3-assige en 5-assige freesmachines.

---

## 3. Gereedschappen (TOOL DEF & TOOL CALL)

### TOOL DEF (Optioneel / Tabeldefinitie)
Definieert de gereedschapsgeometrie direct in het programma als er geen externe gereedschapstabel (`TOOL.T`) gekoppeld is:
```klartext
TOOL DEF 5 L+120 R+5
```
- `L`: Gereedschapslengte ($L$).
- `R`: Gereedschapsradius ($R$, halve freesdiameter).

### TOOL CALL (Gereedschapswissel)
Roept het gereedschap op, start het spiltoerental en stelt de standaardvoeding in:
```klartext
TOOL CALL 5 Z S3500 F800 DL+0 DR+0
```
- `5`: Gereedschapsnummer (of gereedschapsnaam tussen aanhalingstekens, bijv. `"FREES_D10"`).
- `Z`: Werkspil-as.
- `S`: Spiltoerental in omw/min ($\text{RPM}$).
- `F`: Voeding in $\text{mm/min}$.
- `DL`: Lengte-afwijking (slijtagecompensatie).
- `DR`: Radius-afwijking (slijtagecompensatie). Positief maakt de frees effectief groter; negatief maakt hem kleiner.

---

## 4. Baanbewegingen (Cartesiaans & Polair)

### 4.1 Lineaire Verplaatsing (`L`)
Lineaire beweging naar een doelpositie.
```klartext
L X+50 Y+30 Z-5 RL F500 M3
L IX+10 IY-5 R0 FMAX
```
- `X`, `Y`, `Z`: Absolute doelcoördinaten ten opzichte van het werkstuknulpunt ($X0, Y0, Z0$).
- `IX`, `IY`, `IZ`: Incrementele verplaatsingen ten opzichte van de huidige positie.
- **Radiuscompensatie**:
  - `R0`: Ongecorrigeerd; het freesmiddelpunt volgt de geprogrammeerde baan (bijv. voor sleuven of boren).
  - `RL`: Gereedschap links van de contour in freesrichting (meelopend frezen bij een buitencontour tegen de klok in).
  - `RR`: Gereedschap rechts van de contour in freesrichting.
- **Voeding**:
  - `F500`: Voeding in $\text{mm/min}$.
  - `FMAX`: IJlgang (maximale machinesnelheid, niet snijdend).
  - `FAUTO`: Neemt automatisch de voeding over uit de actieve `TOOL CALL`.

### 4.2 Poolcoördinaten (`CC`, `LP`, `CP`)
Ideaal voor cirkel- en boogpatronen, schuine vlakken en regelmatige veelhoeken.

1. **Pool vastleggen (`CC`)**:
   ```klartext
   CC X+50 Y+50     ; Pool op absoluut middelpunt
   CC               ; Neemt de HUIDIGE gereedschapspositie direct als pool over!
   ```
2. **Lineair Polair (`LP`)**:
   Beweegt in een rechte lijn naar een punt gedefinieerd door straal en hoek.
   ```klartext
   LP PR+40 PA+108 R0 F1000
   LP IPA+72 R0      ; Incrementele hoekverdraaiing
   ```
   - `PR`: Poolradius (afstand vanaf het `CC`-punt).
   - `PA`: Poolhoek in graden ($0^\circ = +X$, tegen de klok in naar $+Y = 90^\circ$).
   - `IPA`: Incrementele poolhoek (draait opgeteld bij de vorige poolhoek).

3. **Cirkel Polair (`CP`)**:
   Beweegt over een cirkelboog rond het `CC`-punt.
   ```klartext
   CP PA+180 DR+
   ```

### 4.3 Cirkelbanen (`C`, `CR`, `CT`)

1. **Cirkel met Middelpunt (`C`)**:
   Vereist een vooraf ingestelde `CC`.
   ```klartext
   CC X+50 Y+50
   C X+75 Y+50 DR+
   ```
   - `DR+`: Cirkelbaan tegen de klok in (Counter-Clockwise).
   - `DR-`: Cirkelbaan met de klok mee (Clockwise).

2. **Cirkel met Straal (`CR`)**:
   Geen `CC` nodig; de boog wordt berekend uit beginpunt, eindpunt en radius $R$.
   ```klartext
   CR X+80 Y+40 R+25 DR-
   ```
   - `R+`: Kleine cirkelboog ($\le 180^\circ$).
   - `R-`: Grote cirkelboog ($> 180^\circ$).

3. **Tangentiële Cirkelboog (`CT`)**:
   Sluit vloeiend (zonder knik) aan op het voorgaande baanelement:
   ```klartext
   CT X+60 Y+80
   ```

---

## 5. Contour Aanrijden & Verlaten (APPR & DEP)

Om aanzetmarkeringen op het werkstuk te voorkomen, rijdt het gereedschap via een zachte tangentiale of loodrechte boog aan en weg, waarbij de radiuscompensatie (`RL`/`RR`) vloeiend wordt ingeschakeld.

| Commando | Beschrijving | Typische parameters |
| :--- | :--- | :--- |
| **`APPR LT`** | Benaderen via een rechte lijn rakend aan het eerste contourelment | `APPR LT X... Y... LEN... RL F...` |
| **`APPR LN`** | Benaderen via een rechte lijn loodrecht op het eerste contourelment | `APPR LN X... Y... LEN... RL F...` |
| **`APPR CT`** | Benaderen via een cirkelboog die raakt aan de contour | `APPR CT X... Y... CCA... R... RL F...` |
| **`APPR LCT`**| Benaderen via een rechte lijn gevolgd door een tangentiële boog | `APPR LCT X... Y... R... RL F...` |
| **`DEP LT`** | Verlaten via een rechte lijn rakend aan het laatste contourelment | `DEP LT LEN... F...` |
| **`DEP LN`** | Verlaten via een rechte lijn loodrecht op het laatste contourelement | `DEP LN LEN... F...` |
| **`DEP CT`** | Verlaten via een cirkelboog rakend aan de contour | `DEP CT CCA... R... F...` |
| **`DEP LCT`** | Verlaten via een tangentiële boog gevolgd door een rechte lijn | `DEP LCT R... F...` |

### Wiskundige werking van `APPR LT`:
Bij `APPR LT X... Y... LEN... RL` berekent de besturing een hulppunt $P_H$ buiten het werkstuk:
$$P_H = P_A - \vec{t}_{\text{contour}} \cdot \text{LEN}$$
Het gereedschap beweegt in $R0$ naar $P_H$ en activeert tussen $P_H$ en het beginpunt $P_A$ geleidelijk de compensatievector $\vec{n} \cdot R_{\text{tool}}$.

---

## 6. Hoekafrondingen & Afschuiningen (RND & CHF)

Voegt automatisch een afronding of velling toe op de hoek tussen twee baanelementen:
```klartext
L X+50 Y+100 RL
RND R8              ; Vloeiende cirkelboogstraal R8 tussen twee lijnen
L X+100 Y+50
CHF 5               ; 45° afschuining met lengte 5 mm
L X+100 Y+0
```

---

## 7. Subprogramma's & Herhalingen (LBL)

### 7.1 Subprogramma (Onderprogramma)
Wordt geplaatst aan het einde van het programma (na `M30`):
```klartext
; Hoofdprogramma
CALL LBL 1
L Z+100 R0 FMAX M30

; Subprogramma definitie
LBL 1
L X+10 Y+10
L Z-5
L Z+5
LBL 0               ; Einde subprogramma (Return)
```

### 7.2 Programmagedeelte-herhaling (Section Repeat)
Herhaalt een blok code meerdere keren zonder aparte `LBL 0`:
```klartext
LBL 2
L IZ-2 F500         ; Verlaag Z incrementeel met 2 mm
... (frees contour)
CALL LBL 2 REP 4    ; Herhaal 4 keer extra (totaal 5 bewerkingen)
```

---

## 8. Vaste Bewerkingscycli (Canned Cycles)

Een cyclus wordt eerst gedefinieerd (`CYCL DEF`) en vervolgens geactiveerd via `CYCL CALL` of positie-gerelateerde M-functies (`M99`, `M89`).

### 8.1 Boorcycli
- **Cyclus 240 (Centreren)**:
  - `Q200`: Veiligheidsafstand (boven oppervlak)
  - `Q201`: Diepte (of `Q343`: Diameter centrering bij $90^\circ$)
  - `Q206`: Aanzetvoeding
- **Cyclus 200 (Boren)**:
  - Standaard enkelvoudig boren met spaanbreken of directe terugtrekking.
- **Cyclus 206 (Draadtappen met compensatiekop)**:
  - Voeding $F = S \times P$. De spilas keert op de bodem automatisch om ($M4$).
- **Cyclus 207 (Draadtappen star / Zonder compensatiekop)**:
  - Spiltoerental en voeding zijn digitaal gesynchroniseerd.
- **Cyclus 208 (Boorfrezen - Helical Bore Milling)**:
  - Freest cirkelvormige gaten uit met een vingerfrees via een continue schroeflijn (helix) met nominale diameter `Q335` en aanzet per omwenteling `Q334`.

### 8.2 Patrooncycli
- **Cyclus 220 (Gatencirkel / Steekcirkel)**:
  - `Q216`, `Q217`: Middelpunt $X$ en $Y$
  - `Q244`: Steekcirkeldiameter
  - `Q245`: Starthoek
  - `Q246`: Eindhoek
  - `Q247`: Hoekstap
  - `Q241`: Aantal boringen
  - Voert de vooraf gedefinieerde boorcyclus automatisch uit op alle berekende punten.
- **Cyclus 221 (Gatenrij / Rechthoekig patroon)**:
  - Patroon langs rijen en kolommen met instelbare afstand en rotatiehoek.

### 8.3 Kamer- en Vlakfreescycli
- **Cyclus 251 (Rechthoekige Kamer)**:
  - Volledige uitruiming en wandnavolging van rechthoekige holtes met afgeronde hoeken.
- **Cyclus 252 (Ronde Kamer)**:
  - Spiraalvormige uitruiming van ronde holtes met instelbare overdekking (`Q370`).
- **Cyclus 253 (Sleuffrezen)**:
  - Pendelend insteken en sleufcontourfrezen.
- **Cyclus 232 / 233 (Vlakfrezen)**:
  - Vlakschillen van het werkstuk in meervoudige banen met instelbare oversteek.

---

## 9. SL-Cycli (Subcontourlijsten)

Maakt complexe samengestelde kamers en eilanden mogelijk via overlappende subcontouren:
1. `CYCL DEF 14.0 CONTOUR`: Bepaalt welke `LBL` nummers de contouren bevatten.
2. `CYCL DEF 20 CONTOURDATA`: Snijdieptes, overdekking, overmaten voor wand en bodem.
3. `CYCL DEF 22 UITRUIMEN`: Ruimt het binnengebied automatisch uit met spiraalbanen.
4. `CYCL DEF 23 BODEMNAVOLGEN`: Werkt de bodem fijn na.
5. `CYCL DEF 24 ZIJKANTNAVOLGEN`: Werkt de contourwanden fijn na op de exacte maat.

---

## 10. Coördinatentransformaties (Cycli 7, 8, 10)

- **Cyclus 7 (Nulpuntverschuiving)**:
  ```klartext
  CYCL DEF 7.0 NULPUNT
  CYCL DEF 7.1 X+50
  CYCL DEF 7.2 Y+50
  CYCL DEF 7.3 Z+0
  ```
- **Cyclus 8 (Spiegelen)**:
  Spiegelt freesbanen over de $X$-, $Y$- of $Z$-as.
  ```klartext
  CYCL DEF 8.0 SPIEGELEN
  CYCL DEF 8.1 X
  ```
- **Cyclus 10 (Rotatie)**:
  Draait het actieve coördinatensysteem over een hoek in graden rond het actieve nulpunt.
  ```klartext
  CYCL DEF 10.0 ROTATIE
  CYCL DEF 10.1 ROT+45
  ```

---

## 11. Q-Parameters & Rekenfuncties (FN)

Q-parameters ($Q0 \dots Q1999$) fungeren als variabelen voor parametrisch programmeren.

### 11.1 Basis Rekenkundige Functies (`FN 0` tot `FN 5`)
- `FN 0: Q1 = +50` $\to$ Directe toewijzing: $Q_1 = 50$.
- `FN 1: Q1 = +Q2 + +Q3` $\to$ Optelling: $Q_1 = Q_2 + Q_3$.
- `FN 2: Q1 = +Q2 - +Q3` $\to$ Aftrekking: $Q_1 = Q_2 - Q_3$.
- `FN 3: Q1 = +Q2 * +Q3` $\to$ Vermenigvuldiging: $Q_1 = Q_2 \times Q_3$.
- `FN 4: Q1 = +Q2 / +Q3` $\to$ Deling: $Q_1 = \frac{Q_2}{Q_3}$.
- `FN 5: Q1 = SQRT +Q2` $\to$ Vierkantswortel: $Q_1 = \sqrt{Q_2}$.

### 11.2 Goniometrie (`FN 6` tot `FN 8`)
- `FN 6: Q1 = SIN +Q2` $\to$ Sinus: $Q_1 = \sin(Q_2^\circ)$.
- `FN 7: Q1 = COS +Q2` $\to$ Cosinus: $Q_1 = \cos(Q_2^\circ)$.

### 11.3 Voorwaardelijke Sprongen (`FN 9` tot `FN 12`)
- `FN 9: IF +Q1 EQU +Q2 GOTO LBL 5` $\to$ Spring naar LBL 5 als $Q_1 == Q_2$.
- `FN 10: IF +Q1 NE +Q2 GOTO LBL 5` $\to$ Spring naar LBL 5 als $Q_1 \neq Q_2$.
- `FN 11: IF +Q1 GT +Q2 GOTO LBL 5` $\to$ Spring naar LBL 5 als $Q_1 > Q_2$.
- `FN 12: IF +Q1 LT +Q2 GOTO LBL 5` $\to$ Spring naar LBL 5 als $Q_1 < Q_2$.

### 11.4 Vrije Wiskundige Formules
De interpreter ondersteunt tevens inline wiskundige expressies:
```klartext
Q10 = (Q1 * COS(Q2)) + (Q3 / 2)
```

---

## 12. M-Hulpfuncties

| M-Code | Betekenis | Werking in Simulator |
| :--- | :--- | :--- |
| **`M0` / `STOP`** | Programmastop | Pauzeert de uitvoering; hervatten met NC START |
| **`M2` / `M30`** | Programma-einde & Terugloop | Stopt spindel/koeling en reset programmateller naar regel 0 |
| **`M3`** | Spil rechtsom (CW) | Activeert spilstatus CW in DRO |
| **`M4`** | Spil linksom (CCW) | Activeert spilstatus CCW in DRO |
| **`M5`** | Spil stop | Schakelt spildraaiing uit |
| **`M8`** | Koelmiddel AAN | Activeert koelwaterindicator in statusbalk |
| **`M9`** | Koelmiddel UIT | Schakelt koeling uit |
| **`M13`** | Spil CW + Koeling AAN | Combineert `M3` en `M8` |
| **`M14`** | Spil CCW + Koeling AAN | Combineert `M4` en `M8` |
| **`M89`** | Modale cyclusaanroep | Voert de actieve cyclus uit op elk volgend positieblok |
| **`M99`** | Cyclusaanroep | Voert de actieve cyclus eenmalig uit op de huidige positie |
