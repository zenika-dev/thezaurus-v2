# Contribuer à TheZaurus-v2

## Convention de commit

Le projet suit **[Conventional Commits](https://www.conventionalcommits.org/fr/v1.0.0/)**.
Les versions, les CHANGELOG et les release notes sont générés à partir des messages de commit par [semantic-release](https://github.com/semantic-release/semantic-release) : un message mal formé n'apparaîtra pas dans la release.

### Format

```
type(scope): description courte à l'impératif

Corps optionnel : le pourquoi du changement.

BREAKING CHANGE: description de la rupture (optionnel)
```

Exemples :

```
feat(front): ajoute la recherche par conférence
fix(api): corrige la pagination des talks
feat(agent)!: remplace le format de réponse
```

### Types et impact sur la version

| Type | Usage | Version |
|---|---|---|
| `feat` | Nouvelle fonctionnalité | mineure (`1.2.0` → `1.3.0`) |
| `fix` | Correction de bug | patch (`1.2.0` → `1.2.1`) |
| `perf` | Amélioration de performance | patch |
| `revert` | Annulation d'un commit | patch |
| `docs`, `style`, `refactor`, `test`, `build`, `ci`, `chore` | Le reste | aucune release |

Un `!` après le type/scope, ou un pied de page `BREAKING CHANGE:`, déclenche une version **majeure** (`1.2.0` → `2.0.0`).

### Scope

Le scope indique le service concerné : `api`, `front`, `agent`, `ci`… Il est recommandé mais pas obligatoire.

Chaque service (`api/`, `front/`, `agents/`) a sa propre version (tags `api-vX.Y.Z`, `front-vX.Y.Z`, `agent-vX.Y.Z`). Un commit n'entre dans la release d'un service **que s'il modifie des fichiers de son dossier**, quel que soit le scope écrit.

### Pull requests

Le **titre de la PR** doit respecter la convention : il est vérifié par le workflow `Lint PR` et devient le message du commit lors du merge en squash. Si la PR ne contient qu'un seul commit, son message doit être identique au titre.

## Release

La release n'est pas automatique au merge : elle est lancée **manuellement, après décision de l'équipe**.

1. Onglet **Actions** → workflow **Release** → **Run workflow** sur `main`.
2. Cocher les services à livrer (**API et front**, **Agent**).
3. Le workflow :
   - lance les tests ;
   - crée pour chaque service ayant des commits `feat`/`fix`/`perf` une release GitHub, un tag et une entrée dans son `CHANGELOG.md` ;
   - crée une release globale `vAAAA.MM.JJ-<sha>` qui regroupe les release notes des services ;
   - déploie sur GCP.
