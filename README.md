# Learning Analysis Tool

Analyzes **what was taught** in a session vs. **what each student understood**,
by comparing the lesson's concepts against the evidence students leave in the
Google Meet chat.

Built for the *Understand · Analyze · Elevate* challenge.

---

## What it does

1. Reads the **lesson** (teacher's `.docx` via Apache POI, or a `.txt` transcript).
2. Extracts the **concepts taught** with an LLM (the ground truth).
3. Parses the **Meet chat** export into per-student message bundles.
4. Judges each student's understanding of each concept on a 4-point scale
   (absent / partial / understood / mastery) **with evidence**.
5. Produces a **dashboard**: per-student coverage %, strengths, gaps, curiosity,
   and **class-wide weak spots** (what to re-teach) — plus **Excel export**.

## Architecture

```
lesson (.docx via POI | .txt transcript)
        │  LLM: extract concepts
        ▼
   concept list ─────────────┐
                             │  LLM: assess each student vs concepts
Meet chat (.txt) ─ parse ─ per-student bundles
                             ▼
   coverage % · gaps · strengths · curiosity · class weak spots
        │
   web dashboard  +  Excel export
```

| Layer         | Class                    | Job                                       |
|---------------|--------------------------|-------------------------------------------|
| Input         | `WordExtractor`          | Read teacher `.docx`                       |
| Input         | `ChatParser`             | Group Meet chat by student                 |
| Intelligence  | `LlmClient`, `Prompts`   | Concept extraction & understanding grading |
| Analysis      | `Summarizer`             | Coverage %, gaps, class weak spots         |
| Service       | `AnalysisService`        | Orchestrates the pipeline                  |
| Web           | `AnalysisController`     | Upload UI, dashboard, Excel export         |

## Requirements

- Java 21+
- Maven 3.8+
- (optional) `ANTHROPIC_API_KEY` for live analysis. Without it the app runs in
  deterministic **mock mode** so you can demo and test with no key.

## Run the web app

```bash
mvn spring-boot:run
```
Open **http://localhost:8080**, upload a lesson file + a Meet chat `.txt`, and
view the dashboard. Use **Export Excel** to download the results.

For live analysis:
```bash
export ANTHROPIC_API_KEY=sk-ant-...
mvn spring-boot:run
```

## Run the tests (Test Evidence)

```bash
mvn test
```
Covers chat parsing, scoring math, edge cases, and the end-to-end pipeline on a
golden sample. No API key needed.

## Getting the inputs from Google Meet

- **Lesson:** use the teacher's Word doc directly, **or** record the Meet
  (Workspace saves the recording to Drive), transcribe the audio with Whisper
  into a `.txt`.
- **Chat:** when the Meet is recorded, Google saves the chat log to Drive as a
  `.txt` (`timestamp  Name: message` per line) — exactly what `ChatParser` reads.
  If recording is off, a participant copies the chat panel before the meeting ends.

## Deliverables in this repo

| Deliverable        | Location                          |
|--------------------|-----------------------------------|
| HLD Document       | `docs/1_HLD_Document.pdf`         |
| Working Solution   | this Spring Boot app              |
| Testing Document   | `docs/2_Testing_Document.pdf`     |
| Accuracy Report    | `docs/3_Accuracy_Report.pdf`      |
| Test Evidence      | `src/test/...AnalysisTest.java`   |
| README             | this file                         |
| Sample data        | `sample_data/`                    |

## Sample data

`sample_data/lesson.txt` + `sample_data/sample_chat.txt` — upload these to see a
full analysis (Priya 75%, Rahul 25%, Amit 8%, class weak spot: rate factors).

## Known limitation (stated honestly)

Chat is a **partial signal**. A quiet student isn't necessarily lost, so
`absent` means *no chat evidence*, not *no understanding*. For fuller coverage,
pair chat with a short post-session quiz. This is by design and documented in
the Accuracy Report.
