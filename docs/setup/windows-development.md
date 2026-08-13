# Windows development setup

## Initial toolchain check

Before the local toolchain installation, this Windows environment had the following observed state:

| Command | Observed pre-install state |
| --- | --- |
| `java -version` | Oracle JRE 8 |
| `javac -version` | Command unavailable (no JDK compiler) |
| `mvn -version` | Command unavailable (Maven not installed) |
| `docker version` | Command unavailable (Docker not installed) |
| `docker compose version` | Command unavailable (Docker Compose not installed) |

## Current verified Java and Maven state

Microsoft Build of OpenJDK 21 is installed at `C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot`. Apache Maven 3.9.16 is installed at `C:\Users\fyq\tools\apache-maven-3.9.16`; it was obtained from the official Apache archive and SHA-512 verification was completed during installation.

The inherited Codex shell still resolves `java` to Oracle JRE 8 (`C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe`). This is a shell PATH inheritance issue, not evidence that the JDK 21 installation failed. For this verification only, set `JAVA_HOME` and prepend its `bin` directory to `PATH` in the current PowerShell process:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;C:\Users\fyq\tools\apache-maven-3.9.16\bin;$env:Path"
java -version
javac -version
mvn -version
```

Verified result on 2026-08-13: `java -version` reported Microsoft OpenJDK `21.0.12` LTS, `javac -version` reported `21.0.12`, and `mvn -version` reported Apache Maven `3.9.16` running on Java `21.0.12`.

## Docker Desktop status

Docker Desktop is not yet verified as installed or usable in this environment. At verification time, `docker version`, `docker compose version`, and `docker run --rm hello-world` could not run because the `docker` command was not found. Do not treat the background Docker Desktop BITS download as a completed installation.

After the download finishes, complete the remaining steps interactively:

1. Run the official Docker Desktop installer and complete its prompts.
2. If prompted, enable or install WSL 2, then restart Windows if requested.
3. Launch Docker Desktop, accept its first-run terms, and select/use the WSL 2 based engine.
4. Wait for Docker Desktop to report that its engine is running.
5. In a new PowerShell session, verify `docker version`, `docker compose version`, and `docker run --rm hello-world` all succeed.

Only after all three verification commands succeed should Docker be recorded as ready for this project.
