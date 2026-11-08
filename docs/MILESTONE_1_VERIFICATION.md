# Milestone 1 verification

Verified 8 November 2026. Maven `clean verify`: BUILD SUCCESS.
JDK: Eclipse Temurin 21.0.13+9; Maven 3.9.11; compiler release 17.
JUnit 5.11.4; Surefire 3.5.2; JaCoCo 0.8.12.

JUnit results: 34 test invocations, 0 failures, 0 errors, 0 skipped.

- Instruction: 110/110 (100%).
- Branch: 8/8 (100%).
- Line: 30/30 (100%).
- Method: 12/12 (100%).

These coverage values concern only the six implemented classes. No coverage of future account
operations is implied. Parameterized cases are counted separately from test methods.

Environment setup used a workspace-local JDK and Maven because the environment initially
provided a JRE without javac and no Maven. The local JDK uses the environment's existing Java
CA trust store; Maven uses inherited proxy settings via an external settings file. None of
these machine-specific settings or downloads are committed. On a normal JDK/Maven installation,
run `mvn clean verify` as documented in README.

Source inspection confirms no main method, Scanner, System output or clock reads. Current UML
was rendered with Graphviz and visually inspected. The interim DOCX is structurally checked;
no claim of full Word/LibreOffice layout rendering is made.
