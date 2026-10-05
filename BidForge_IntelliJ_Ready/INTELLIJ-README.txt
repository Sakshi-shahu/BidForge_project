BIDFORGE - INTELLIJ IDEA SETUP

1. Extract this ZIP completely to a normal folder (do not run from inside the ZIP).
2. Open IntelliJ IDEA.
3. Select File > Open.
4. Select the extracted BidForge_IntelliJ_Ready folder OR its pom.xml.
5. If prompted, choose Open as Project and Load Maven Project.
6. Set Project SDK to Java 21: File > Project Structure > Project SDK.
7. Wait for Maven dependencies to finish downloading.
8. Open src/main/java/com/bidforge/rfq/BidForgeApplication.java.
9. Run BidForgeApplication using the green Run icon.
10. Ensure MySQL is running and the database credentials in
    src/main/resources/application.properties match your local MySQL setup.

Maven terminal commands (from this folder):
  Windows: .\\mvnw.cmd clean package -DskipTests
  macOS/Linux: ./mvnw clean package -DskipTests

Important:
- Do not open an individual .java file as a project.
- Do not copy only the src folder; keep pom.xml at the project root.
- Java 21 and Maven dependency download access are required.
