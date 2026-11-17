Les fonds PNG proviennent des huit templates de la PR 190 (commit
908b03db2866b999ea403b6b68a3bd0f73903a3a). Les champs variables ont été
effacés et les fonds sous les photos reconstitués lors de la validation locale
du rendu déterministe. Le fond nc-solo conserve un seul emplacement de portrait.

templates.json décrit les dimensions natives, les emplacements et la typographie
mesurée sur ces références. Les polices statiques sont dérivées de Nunito variable
aux graisses utilisées par les templates ; leur licence est dans fonts/OFL.txt.

Le service ne nécessite ni navigateur, ni Node.js, ni génération d'image par IA.
Java2D compose les textes, logos et photos sur ces fonds. Gemini fournit seulement
des coordonnées de cadrage. Les photos de vérification ne font pas partie des assets.

`branding/zenika-mark.png` reprend le symbole circulaire de `templates/blog-2.png`
(zone 378,1174–475,1271), avec un masque circulaire transparent.
Il remplace les photos non transmises, sans analyse Gemini.
