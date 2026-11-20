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

`branding/zenika-mark.svg` conserve le symbole circulaire vectoriel du logo officiel :
https://github.com/Zenika/apps.zenika.com/blob/master/public/img/Logo_Zenika_Noir.svg
Le mot-symbole est retiré et le viewBox limité au symbole ; les tracés et le dégradé sont conservés.
`branding/zenika-mark.png` en est une rasterisation transparente de 1024 × 1024 pixels,
pour éviter tout agrandissement dans les emplacements des templates.
Java2D utilise ce PNG sans dépendance SVG au démarrage.
Il remplace les photos absentes, sans analyse Gemini.

Pour régénérer le PNG depuis la racine du dépôt après installation des dépendances front :

```sh
node -e "require('./front/node_modules/sharp')('agents/visuals-agent/src/main/resources/branding/zenika-mark.svg').resize(1024,1024).png().toFile('agents/visuals-agent/src/main/resources/branding/zenika-mark.png')"
```
