# 🏛️ Civis RJ - Predictive Command Center

![Status](https://img.shields.io/badge/Status-Hackathon_MVP-blue)
![Stack](https://img.shields.io/badge/Stack-React_%7C_Spring_Boot_%7C_Supabase-cyan)
![Integrations](https://img.shields.io/badge/Integrations-Nutrient_DWS_%7C_SerpApi_%7C_OpenRouter-emerald)

**Civis RJ** is a predictive intelligence platform designed for the management and auditing of public works. Acting as an operational command center, the system monitors construction projects in real-time and performs **automated audits of government contracts and public tenders** to prevent non-compliance, delays, and waste of public funds.

---

## 📸 Application Demo & Integrations

<div align="center">
  <h3>System Overview (Command Center)</h3>
  <p>Interactive demonstration of navigation flows and real-time rendering of thousands of public works via Supabase.</p>
  <img src="./assets/civis-rj.gif" alt="Civis RJ Demo" width="800">

  <br><br>

  <h3>Complete Audit Workflow (Sponsor Integrations)</h3>
  <p>100% automated pipeline: Contract Reading (Nutrient DWS) ➔ Predictive Verdict (OpenRouter/Llama 3) ➔ Official Document Generation (Doctavian) ➔ Public Domain Provisioning (Name.com).</p>
  <img src="./assets/auditoria-completa.png" alt="Complete Audit" width="800">

  <br><br>

  <h3>Investigative Due Diligence (OSINT with SerpApi)</h3>
  <p>The system performs a real-time web scan of the contractor's reputation, flagging and blocking contracts if a history of fraud or paralysis is detected.</p>
  <img src="./assets/diligence-serpapi-risk.png" alt="SerpApi Due Diligence" width="800">

  <br><br>

  <h3>Risk Mapping (IBGE + Geolocation)</h3>
  <p>Dashboard with the general overview of indicators, clustering areas with the highest financial risk of delays.</p>
  <img src="./assets/Tela Principal.png" alt="Main Dashboard" width="800">
</div>

---

## 🚀 The Challenge & Our Solution

Manual auditing of public construction contracts takes weeks of technical analysis and is highly susceptible to human error, fraud, or the omission of critical clauses.

In this MVP, we built a robust pipeline integrating the **Nutrient DWS Data Extraction API**, **SerpApi**, and **OpenRouter (LLMs)** to automate this end-to-end workflow:

1. **Security & Context Validation:** The Spring Boot backend intercepts the uploaded PDF, checking its "Magic Bytes" to prevent malicious files, and uses Apache PDFBox to ensure the document contains actual legal context before processing.
2. **Real-Time Due Diligence (SerpApi):** Before approving a contract, the system scrapes the web for news regarding the contractor to detect recent fraud allegations or delays.
3. **Intelligent Extraction (Nutrient DWS):** The document is sent to Nutrient DWS to extract vital structured data (contract values, deadlines, and execution metadata) using a custom JSON Schema.
4. **Human-in-the-Loop & AI Verdict:** The auditor visualizes the extracted data alongside the original document in the DWS Viewer. Upon human approval, our AI Copilot (via OpenRouter) generates a final predictive audit report combining the extracted document data and web intelligence.

---

## 🛠️ Architecture & Technologies

### Frontend
*   React 18 with TypeScript
*   Vite
*   Tailwind CSS + Shadcn UI
*   Lucide React (Icons)

### Backend
*   Java 17+
*   Spring Boot 3 (Spring Web, Spring Data JPA, Spring Validation)
*   Apache PDFBox (Document context validation)
*   PostgreSQL hosted on Supabase
*   HikariCP (Connection Pooling)

### Services & APIs
*   **Nutrient DWS API:** Data Extraction & Interactive Document Viewer
*   **SerpApi:** Real-time Google News scraping for contractor due diligence
*   **OpenRouter API:** LLM routing for the AI Copilot final verdict
*   **Supabase:** Relational database and cloud infrastructure
*   **Doctavian API:** Official legal document generation
*   **Name.com API:** Transparency portal domain provisioning

---

## 📁 Monorepo Structure

```text
civis-rj/
├── backend/                  # REST API in Spring Boot
│   ├── src/main/java/        # Controllers, Services, Repositories, and Entities
│   ├── src/main/resources/   # application.properties
│   └── pom.xml               # Maven Dependencies
├── frontend/                 # Web Application in React + Vite
│   ├── src/components/civis/ # Dashboards, Maps, and Audit Panels
│   ├── src/routes/           # Application Routing
│   └── package.json          # Node Dependencies
├── assets/                   # Images and GIFs for documentation
├── .gitignore
└── README.md
```

---

## ⚙️ How to Run Locally

### Prerequisites
*   Java 17+ and Maven installed
*   Node.js 18+ and npm (or yarn/pnpm)
*   PostgreSQL instance (or a Supabase project)

---

### 1. Database Setup
Run the following SQL script to create the audit table in your PostgreSQL database:

```sql
CREATE TABLE IF NOT EXISTS contract_audits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_obra TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_EXTRACTION',
    dws_document_id VARCHAR(255),
    dws_viewer_url VARCHAR(500),
    extracted_data TEXT,
    auditor_notes TEXT,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_contract_audits_obra FOREIGN KEY (id_obra) REFERENCES obras(id_obra)
);

CREATE INDEX IF NOT EXISTS idx_contract_audits_id_obra ON contract_audits(id_obra);
CREATE INDEX IF NOT EXISTS idx_contract_audits_status ON contract_audits(status);
```

---

### 2. Running the Backend (Spring Boot)

1. Navigate to the backend folder:
   ```bash
   cd backend
   ```

2. Configure your credentials in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://db.YOUR_PROJECT.supabase.co:5432/postgres
   spring.datasource.username=postgres
   spring.datasource.password=YOUR_DB_PASSWORD
   spring.jpa.hibernate.ddl-auto=update
   
   nutrient.api-key=YOUR_NUTRIENT_API_KEY
   nutrient.api-url=[https://api.nutrient.io](https://api.nutrient.io)
   
   serpapi.api-key=YOUR_SERPAPI_KEY
   
   doctavian.template.urn=YOUR_TEMPLATE_URN
   ```

3. Start the server:
   ```bash
   mvn spring-boot:run
   ```
   *(The backend will run on http://localhost:8080)*

---

### 3. Running the Frontend (React)

1. In a new terminal, navigate to the frontend folder:
   ```bash
   cd frontend
   ```

2. Create a `.env` file in the frontend root:
   ```env
   VITE_API_BASE_URL=http://localhost:8080/api/audits
   VITE_OPENROUTER_API_KEY=YOUR_OPENROUTER_API_KEY
   ```

3. Install dependencies and start the development server:
   ```bash
   npm install
   npm run dev
   ```
   *(Access the interface at http://localhost:5173)*

---

## 👥 Developed for the DevNetwork Hackathon
Designed and engineered by Rodrigo Carvalho Lima. Built to modernize public administration, ensure compliance, and leverage AI for flawless document processing and urban management.
