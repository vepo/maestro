# GitHub Actions

## Pipeline overview

```
                    ┌─────────────────────────────────────────┐
  PR / push main ──►│  build.yml — verify + SonarQube         │
                    └─────────────────────────────────────────┘
                                        │
                    manual ─────────────┼──────────────────────► prepare-release.yml
                                        │                              │
                                        │                              ▼ tag v*.*.*
                                        │                    release.yml (Maven + Docker + GH Release)
                                        │
                    manual ─────────────┴──────────────────────► deploy.yml (K8s)
```

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| [build.yml](workflows/build.yml) | PR, push to `main` | `mvn verify` + SonarCloud analysis |
| [prepare-release.yml](workflows/prepare-release.yml) | Manual | Tests, version bump, `v<version>` tag |
| [release.yml](workflows/release.yml) | Tag `v*.*.*` | Publish to GitHub Packages, GHCR, GitHub Release |
| [deploy.yml](workflows/deploy.yml) | Manual | Deploy operator to staging/production |

## Secrets

| Secret | Used by |
|--------|---------|
| `SONAR_TOKEN` | build.yml — SonarCloud upload |
| `GITHUB_TOKEN` | build.yml (PR decoration), release.yml, prepare-release.yml |
| `KUBE_CONFIG` | deploy.yml — base64 kubeconfig |

## Local parity

```bash
mvn verify   # same as the Build workflow (without Sonar)
```

Sonar locally (optional):

```bash
mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.token=$SONAR_TOKEN
```
