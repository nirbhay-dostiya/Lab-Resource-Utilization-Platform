# 🔬 Lab Resource Utilization System

<div align="center">

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Secured-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)

**An enterprise-grade, full-stack platform for intelligent management of laboratory equipment, bookings, billing, maintenance, IoT monitoring, and cross-institutional resource sharing.**

[Features](#-features-at-a-glance) · [Architecture](#-system-architecture) · [Milestones](#-project-milestones) · [Setup](#-getting-started) · [API Docs](#-api-documentation) · [Tech Stack](#-technology-stack)

</div>

---

## 📋 Table of Contents

- [Project Overview](#-project-overview)
- [Features at a Glance](#-features-at-a-glance)
- [System Architecture](#-system-architecture)
- [Technology Stack](#-technology-stack)
- [Project Milestones](#-project-milestones)
  - [Milestone 1 — Foundation: Auth, Institutions and Inventory](#milestone-1--foundation-auth-institutions-and-inventory)
  - [Milestone 2 — Booking, Scheduling and Resource Sharing](#milestone-2--booking-scheduling-and-resource-sharing)
  - [Milestone 3 — Maintenance, Calibration and Billing](#milestone-3--maintenance-calibration-and-billing)
  - [Milestone 4 — Analytics, Persona Dashboards and Notifications](#milestone-4--analytics-persona-dashboards-and-notifications)
  - [Milestone 5 — OEE Reports, IoT Telemetry and Full Frontend Integration](#milestone-5--oee-reports-iot-telemetry-and-full-frontend-integration)
- [Data Model Overview](#-data-model-overview)
- [API Documentation](#-api-documentation)
- [Role-Based Access Control](#-role-based-access-control)
- [Getting Started](#-getting-started)
- [Environment Configuration](#-environment-configuration)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Project Overview

The **Lab Resource Utilization System** is a production-ready, multi-tenant SaaS platform designed for academic institutions, research universities, and industrial labs that need to maximize the utilization of expensive scientific equipment. The system eliminates equipment silos, prevents double-bookings, automates billing across departments and institutions, tracks maintenance compliance, and delivers actionable analytics to every stakeholder from the individual researcher booking a microscope to the System Administrator monitoring platform-wide financial health.

### The Problem It Solves

In any large institution with shared scientific equipment (oscilloscopes, spectrometers, CNC machines, centrifuges, etc.), the typical pain points are:

- **Equipment is either idle or overbooked** — no real-time visibility.
- **Maintenance is reactive, not preventive** — calibration certificates expire silently.
- **Billing is manual and error-prone** — spreadsheets cannot capture real per-booking costs.
- **Cross-institutional sharing is impossible** — no trusted, trackable mechanism exists.
- **Reporting is an afterthought** — managers lack data to justify capex decisions.

This system addresses all of the above through a unified, role-aware platform with live data pipelines.

---

## ✨ Features at a Glance

| Module | Key Capabilities |
|--------|-----------------|
| 🔐 **Auth and RBAC** | JWT authentication, 7 distinct roles, fine-grained `@PreAuthorize` on every endpoint |
| 🏛️ **Institution Mgmt** | Multi-tenant hierarchy: Institution → Department → User |
| 📦 **Equipment Inventory** | Category-based registry with status lifecycle, tag system, location tracking |
| 📅 **Booking and Scheduling** | Calendar UI, recurring series, conflict detection, auto-waitlist |
| 🤝 **Resource Sharing** | Cross-institution listing, sharing agreements, access request workflow |
| 🔧 **Maintenance and Calibration** | Work order Kanban, state machine lifecycle, calibration compliance dashboard |
| 💰 **Cost and Billing** | Auto-invoice generation, funding source tracking, overhead rates, PDF/CSV export |
| 📊 **Analytics** | Per-role dashboards, KPI drill-downs, heatmaps, utilization charts |
| 📈 **OEE Reports** | Async report engine, PDF and CSV export, real-time poll-to-download flow |
| 🌡️ **IoT Telemetry** | Sensor log ingestion, time-series indexing, daily utilization metric rollups |
| 🔔 **Notifications** | Event-driven email (Spring Mail + Thymeleaf templates) with user preferences |

---

## 🏗️ System Architecture

```
+---------------------------------------------------------------------+
|                        React 19 (Vite + TailwindCSS)               |
|  LoginPage  |  SignupPage  |  Dashboard (Single-Page, 5750+ lines) |
|  FullCalendar | Recharts | D3.js | Chart.js | lucide-react icons    |
+---------------------------+-----------------------------------------+
                            |  HTTP/REST via Axios
                            v
+---------------------------------------------------------------------+
|               Spring Boot 4.1 (MVC + Spring Security)              |
|                                                                     |
|  auth_user        booking_scheduling   equipment_inventory          |
|  (JWT/BCrypt)     (Calendar/Conflict)  (Status Lifecycle)          |
|                                                                     |
|  maintenance      analytics_reporting  resource_sharing             |
|  (WorkOrder SM)   (OEE/Dashboards)     (Listing/Agreement)         |
|                                                                     |
|  cost_billing     notification         iot_utilization_monitoring   |
|  (Invoice/Budget) (Email+In-App)       (Telemetry/Daily Rollup)    |
+---------------------------------------------------------------------+
                              |  Spring Data JPA / Hibernate
                              v
                    +------------------+
                    |  PostgreSQL 15   |
                    |  (LabResourceDB) |
                    +------------------+
```

The backend is organized as a **vertical-slice / domain-module** architecture. Each business domain is a self-contained package with its own `controller`, `service`, `repository`, `entity`, `dto`, and `enums`. This makes the system highly maintainable and ready for extraction into microservices if the organization scales beyond a monorepo.

---

## 🧰 Technology Stack

### Backend

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 (LTS) | Core language |
| Spring Boot | 4.1.0 | Application framework |
| Spring Security | 4.x | Authentication and authorization |
| Spring Data JPA / Hibernate | 4.x | ORM and database access |
| Spring Mail | 4.x | Email notifications |
| Thymeleaf | 4.x | HTML email templates |
| JJWT | 0.11.5 | JWT token generation and validation |
| PostgreSQL | 15 | Relational database |
| Lombok | Latest | Boilerplate elimination |
| OpenPDF | 1.3.30 | PDF report generation |
| SpringDoc OpenAPI | 2.5.0 | Swagger UI / API documentation |
| Spring Dotenv | 4.0.0 | `.env` file support |
| Maven | 3.x | Build and dependency management |

### Frontend

| Technology | Version | Purpose |
|-----------|---------|---------|
| React | 19 | UI framework |
| Vite | 8.1 | Build tool and dev server |
| React Router DOM | 7.x | Client-side routing |
| Axios | 1.x | HTTP client |
| FullCalendar | 6.1 | Interactive booking calendar |
| Recharts | 3.x | Analytics bar/line charts |
| Chart.js + react-chartjs-2 | 4.x | Additional chart types |
| D3.js | 7.x | Custom data visualizations |
| Lucide React | 1.x | Icon system |
| React Hot Toast | 2.x | Toast notifications |
| TailwindCSS | 4.x | Utility-first styling |

---

## 🚀 Project Milestones

The system was built incrementally across **5 well-defined milestones**, each delivering shippable vertical slices of functionality. Every milestone includes a backend REST API, a database schema, and the corresponding frontend UI.

---

## Milestone 1 — Foundation: Auth, Institutions and Inventory

**Goal:** Establish the security backbone, multi-tenant organization hierarchy, and the equipment registry that every other module depends on.

### 🔐 Authentication and Authorization (`auth_user` module)

- **JWT-based stateless authentication** using `JJWT 0.11.5` with BCrypt password hashing.
- **Custom `UserDetailsService`** and `JwtAuthenticationFilter` that intercepts every request and validates the Bearer token before it reaches any controller.
- **`SecurityConfig`** with method-level security (`@PreAuthorize`) and CORS rules configured for the React frontend.
- **Four authentication endpoints:**
  - `POST /api/auth/register` — General user registration
  - `POST /api/auth/register/institution` — Institution-level admin self-registration
  - `POST /api/auth/register/student` — Simplified student onboarding
  - `POST /api/auth/login` — Credential validation, returns JWT token
  - `GET /api/auth/profile` — Returns the authenticated user's full profile including roles, department, and institution

- **Role hierarchy (`RoleType` enum):**
  ```
  SYSTEM_ADMIN → INSTITUTION_ADMIN → DEPT_HEAD → LAB_MANAGER → LAB_TECHNICIAN → RESEARCHER → STUDENT
  ```
  Each role is stored in a `roles` table with a `user_roles` join table (Many-to-Many). The `Permission` entity enables fine-grained action-level authorization.

- **User entity** stores `email`, `password_hash`, `first_name`, `last_name`, `is_active`, `oauth_provider`, and FK links to both an `Institution` and `Department`.

### 🏛️ Institution and Department Management (`institution` module)

- Full CRUD for `Institution` (name, address, domain, contact details).
- Full CRUD for `Department` scoped under an institution.
- Admin-only endpoints guard institution creation and user assignment.
- Users are linked to their institution and department at registration or by admin assignment.

### 📦 Equipment Inventory (`equipment_inventory` module)

- **`Equipment` entity** with fields: name, model, serial number, location, status (`AVAILABLE`, `BOOKED`, `UNDER_MAINTENANCE`, `DECOMMISSIONED`), hourly rate, purchase date, warranty expiry, and FK relationships to `EquipmentCategory`, `Department`, and `Institution`.
- **`EquipmentCategory`** for grouping (e.g., Microscopes, Spectrometers, CNC Machines).
- **Tag system** (`Tag` entity) for flexible searching and filtering.
- **Status lifecycle** enforced at the service layer — equipment status transitions are validated.

### 🖥️ Frontend (Milestone 1 UI)

- **Login Page** (`LoginPage.jsx`) — JWT token stored in `AuthContext`, redirects to dashboard on success.
- **Signup Page** (`SignupPage.jsx`) — Role-aware registration form.
- **Dashboard shell** with sidebar navigation, profile section, and initial institution/department/equipment management panels.
- **`ProtectedRoute`** component — redirects unauthenticated users to `/login`.

---

## Milestone 2 — Booking, Scheduling and Resource Sharing

**Goal:** Allow researchers to discover and book equipment, handle conflicts gracefully via a waitlist, and enable institutions to share their assets with external partners.

### 📅 Booking and Scheduling (`booking_scheduling` module)

- **`Booking` entity** — links a `User` to an `Equipment` with `start_time`, `end_time`, `status` (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`, `NO_SHOW`), and an optional `purpose` text.
- **`BookingSeries`** — recurring booking groups. A single series owns many individual `Booking` records.
- **`BookingStatusHistory`** — immutable audit log of every status transition with timestamp and actor.
- **`Waitlist` entity** — if equipment is booked during a requested slot, the user is placed in a position-ordered waitlist. When a booking is cancelled, the next waitlist entry is automatically promoted to `PENDING`.

- **Conflict Detection:** Service-layer overlap check prevents double-booking using JPA range queries.

- **Key API endpoints:**
  - `GET /api/bookings` — user's own bookings (paginated, filterable by status)
  - `GET /api/bookings/all` — admin view of all bookings
  - `POST /api/bookings` — create a booking (conflict-checked)
  - `PATCH /api/bookings/{id}/cancel` — cancel with status history log
  - `GET /api/bookings/waitlists` — user's active waitlist entries
  - `GET /api/bookings/occupied-dates` — dates blocked for calendar rendering

### 🤝 Cross-Institutional Resource Sharing (`resource_sharing` module)

- **`SharingAgreement`** — a formal, bilateral contract between two institutions governing shared access.
- **`SharedEquipmentListing`** — publishes a specific piece of equipment for external access with an `external_hourly_rate`, `terms_and_conditions`, and availability window.
- **`AccessRequest`** — external researchers submit requests; owning institution managers approve or deny.

- **Workflow:**
  1. `LAB_MANAGER` posts equipment: `POST /api/sharing/listings`
  2. External researcher discovers it: `GET /api/sharing/listings`
  3. Researcher submits access request: `POST /api/sharing/access-requests`
  4. Owning institution approves: `PATCH /api/sharing/access-requests/{id}/approve`
  5. On approval, a booking is auto-created for the requester.

### 📊 Utilization Heatmap and Performance

- `GET /api/utilization/heatmap` — day-of-week × hour-of-day matrix of booking frequency.
- `GET /api/utilization/performance` — top-N most-used and bottom-N idle equipment lists.

### 🖥️ Frontend (Milestone 2 UI)

- **FullCalendar integration** — interactive day/week/month grid with booking event overlays.
- **Booking modal** — equipment picker, date/time range, purpose text, conflict feedback.
- **Waitlist modal** — shows current position and expected availability.
- **Shared Resources panel** — list of external listings with "Request Access" button.
- **Heatmap view** — D3/Recharts visualization of peak usage hours.

---

## Milestone 3 — Maintenance, Calibration and Billing

**Goal:** Close the operational loop — track equipment health, enforce regulatory calibration compliance, and automate financial billing across department chargebacks and external invoicing.

### 🔧 Maintenance Management (`maintenance_calibration` module — Part 1)

- **`MaintenanceTask` (Work Order) entity:**
  - `maintenanceType`: `PREVENTIVE`, `CORRECTIVE`, `EMERGENCY`
  - `priority`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
  - `status` with **state machine lifecycle**:
    ```
    CREATED → ASSIGNED → IN_PROGRESS → ON_HOLD → COMPLETED → CLOSED
    ```
  - `downtimeHours` — locked in at `COMPLETED` transition, feeds into OEE calculations.
  - `cost` — tracks financial cost of the maintenance event.

- **Key API endpoints:**
  - `POST /api/maintenance` — raise a new work order
  - `GET /api/maintenance` — list all tasks (filter by status, equipment, technician)
  - `PATCH /api/maintenance/{id}/transition` — advance status with resolution notes and downtime hours

- **Scheduled jobs** — Spring `@Scheduled` runs nightly to flag overdue work orders and trigger notifications.

### 📐 Calibration Compliance (`maintenance_calibration` module — Part 2)

- **`CalibrationRecord` entity** — `calibration_date`, `expiry_date`, `vendor_name`, `status` (`VALID`, `EXPIRED`, `DUE_SOON`, `OVERDUE`), `certificate_url`, `tolerance_metrics`.
- **Compliance Dashboard API:** `GET /api/calibration/compliance-dashboard` — per-equipment compliance status, days-until-expiry, and overdue flag.
- **Booking blocker** — equipment with `EXPIRED` or `OVERDUE` calibration status cannot be booked.

### 💰 Cost and Billing (`cost_billing` module)

- **`Invoice` entity** — billed to EITHER an `Institution` (external) OR a `Department` (internal chargeback), enforced via `@PrePersist` / `@PreUpdate` database-level validation.
- **`InvoiceLineItem`** — individual billing lines per booking or maintenance event.
- **`Transaction`** — payment record with `payment_method`, `reference_number`, `amount`, `status`.
- **`Budget`** — department-level budget envelope with alert thresholds.
- **`FundingSource`** — grant numbers, PO numbers, or institutional budgets.

- **Auto-Invoice Generation:**
  ```
  POST /api/billing/invoices/generate-auto?departmentId=...&periodStart=...&periodEnd=...
  ```
  Aggregates all confirmed bookings and completed maintenance tasks in the billing period, computes line items, and creates a `DRAFT` invoice automatically.

- **Approval Workflow:** `DRAFT` → `SUBMITTED` → `APPROVED` → `PAID`

- **Export:** PDF (via OpenPDF) and CSV formats for invoices.

### 🖥️ Frontend (Milestone 3 UI)

- Work order creation form with Kanban-style list, color-coded priority badges, and status transition modal.
- Calibration records table with compliance status chips and compliance dashboard panel.
- Invoice list with pagination, status filter, and invoice detail modal with full line items.
- Payment simulation modal and auto-invoice generator.

---

## Milestone 4 — Analytics, Persona Dashboards and Notifications

**Goal:** Transform raw operational data into actionable intelligence for every user persona and add an event-driven notification system.

### 📊 Analytics and Reporting (`analytics_reporting` module)

- **Scoped analytics endpoints:**
  - `GET /api/analytics/global` — SYSTEM_ADMIN: platform-wide metrics
  - `GET /api/analytics/institution/{id}` — INSTITUTION_ADMIN: institution-scoped
  - `GET /api/analytics/department/{id}` — DEPT_HEAD / LAB_MANAGER: department-scoped

- **Drill-down endpoints** — paginated detail lists filterable by `EquipmentStatus` or `BookingStatus` at each scope level.

### 🧑‍💼 Persona Dashboards (3 Distinct Views)

| Persona | Endpoint | What They See |
|---------|----------|---------------|
| **Researcher** | `GET /api/analytics/dashboard/researcher?userId=...` | Personal booking history, upcoming reservations, total hours used |
| **Lab Manager / Dept Head** | `GET /api/analytics/dashboard/lab-manager?institutionId=...` | Equipment utilization rates, work order summary, calibration compliance % |
| **System Admin** | `GET /api/analytics/dashboard/system-admin` | Cross-institution financial health, total revenue, outstanding invoices |

### 🔔 Notification System (`notification` module)

- **`Notification` entity** — in-app notification with `type`, `title`, `message`, `is_read`, `metadata` (JSON).
- **`NotificationTemplate`** — reusable message templates for system events.
- **`UserNotificationPreference`** — per-user opt-in for email vs. in-app per notification category.
- **Event-driven dispatch** via Spring `ApplicationEvent` / `@EventListener` — services publish domain events; notification service handles delivery asynchronously.
- **Spring Mail with Thymeleaf HTML templates** for formatted email notifications.

### 🖥️ Frontend (Milestone 4 UI)

- **Top-bar notification bell** — badge counter, dropdown panel, mark-as-read.
- **Role-aware analytics section** — KPI cards with clickable drill-down modals, Recharts bar/line charts.
- **KPI drill-down modals** — paginated, filterable record lists.

---

## Milestone 5 — OEE Reports, IoT Telemetry and Full Frontend Integration

**Goal:** Complete the intelligence layer with an asynchronous OEE report engine, IoT sensor data ingestion, and a polished, fully integrated single-page frontend.

### 📈 OEE Report Engine (`analytics_reporting` module — async extension)

- **`SavedReport` entity** — persists report metadata: `report_type`, `parameters` (JSON), `status` (`PENDING`, `DONE`, `FAILED`), `result_json`, `requester`, timestamps.
- **`ReportExecution`** — execution audit log.
- **Async report worker** — Spring `@Async` with dedicated thread pool (`report-worker-*`, core=2, max=8, queue=100). Long-running OEE computations never block the HTTP thread.

- **OEE Calculation:**
  - **Availability** = (Scheduled Time − Downtime) / Scheduled Time
  - **Performance** = Actual Booking Hours / Theoretical Capacity
  - **Quality** = Completed Bookings / Total Bookings
  - **OEE Score** = Availability × Performance × Quality

- **Poll-to-download report flow:**
  ```
  1. POST /api/reports/generate?from=...&to=...   → { reportId, status: "PENDING" }
  2. GET  /api/reports/{reportId}/status           → { status: "PENDING|DONE|FAILED" }
  3. GET  /api/reports/{reportId}/result           → JSON report data (for UI rendering)
  4. GET  /api/reports/{reportId}/download?format=pdf|csv  → binary download
  ```

- **Export formats:** CSV (raw tabular data) and PDF (formatted report via OpenPDF).

- **`SystemAuditLog`** — every sensitive action written to `system_audit_logs` for compliance.

### 🌡️ IoT Utilization Monitoring (`iot_utilization_monitoring` module)

- **`IotTelemetryLog` entity** — raw sensor readings: `equipment_id`, `recorded_at`, `sensor_status` (`ACTIVE`, `IDLE`, `ERROR`, `OFFLINE`), `reading_value`. **Composite index** on `(equipment_id, recorded_at DESC)` for time-series query performance.
- **`DailyUtilizationMetric`** — pre-aggregated daily rollup populated by nightly Spring `@Scheduled` job.
- **API endpoints:**
  - `POST /api/iot/telemetry` — IoT gateway pushes sensor readings
  - `GET /api/iot/telemetry/{equipmentId}` — retrieve recent readings for an asset
  - `GET /api/iot/metrics/daily` — daily aggregated utilization data

### 🖥️ Frontend — Full Integration (Milestone 5 Completion)

- **Reports section** — async report flow with real-time 2-second status polling, OEE results display, and PDF/CSV download buttons.
- **IoT Live Panel** — equipment sensor status badges, last-reading timestamps, utilization percentage gauges.
- **Billing final polish** — transaction detail modals, full invoice lifecycle UI, funding source selectors.
- **Notification preferences** — user-configurable email/in-app preferences panel.
- **Responsive layout** — collapsible sidebar, mobile-friendly tables, adaptive chart sizing.
- **Error Boundary** — React `ErrorBoundary` class component prevents white-screen crashes.
- **Search and Filter everywhere** — booking history, equipment, invoice, and booking status filters.

---

## 🗄️ Data Model Overview

```
institutions ---< departments ---< users >---< user_roles >--- roles >--- permissions
     |                |               |
     |                +---< equipment-+
     |                      |         |
     |                  bookings <-----+
     |                  booking_series
     |                  booking_status_history
     |                      |
     |              maintenance_tasks (work orders)
     |              calibration_records
     |              iot_telemetry_logs --- daily_utilization_metrics
     |                      |
shared_equipment_listings ---+
sharing_agreements
access_requests
     |
invoices ---< invoice_line_items
         ---< transactions
budgets
funding_sources
     |
saved_reports ---< report_executions
notifications
notification_templates
user_notification_preferences
system_audit_logs
```

---

## 📖 API Documentation

Once the backend is running, interactive Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

Powered by **SpringDoc OpenAPI 2.5.0**, all endpoints are documented with request/response schemas.

### Key API Groups

| Group | Base Path | Auth Required |
|-------|-----------|---------------|
| Authentication | `/api/auth` | No (register/login), Yes (profile) |
| Users | `/api/users` | Yes |
| Institutions | `/api/institutions` | Yes |
| Departments | `/api/departments` | Yes |
| Equipment | `/api/equipment` | Yes |
| Bookings | `/api/bookings` | Yes |
| Waitlists | `/api/bookings/waitlists` | Yes |
| Resource Sharing | `/api/sharing` | Yes |
| Utilization | `/api/utilization` | Yes |
| Maintenance | `/api/maintenance` | Yes |
| Calibration | `/api/calibration` | Yes |
| Billing | `/api/billing` | Yes |
| Analytics | `/api/analytics` | Yes (role-gated) |
| Reports | `/api/reports` | Yes (role-gated) |
| IoT Telemetry | `/api/iot` | Yes |
| Notifications | `/api/notifications` | Yes |

---

## 🔐 Role-Based Access Control

| Role | Description | Key Permissions |
|------|-------------|----------------|
| `SYSTEM_ADMIN` | Platform superuser | Full access to all institutions, global analytics, user role assignment |
| `INSTITUTION_ADMIN` | Manages a single institution | Institution CRUD, all departments, all equipment within institution |
| `DEPT_HEAD` | Manages a department | Department equipment, department bookings, approve invoices |
| `LAB_MANAGER` | Manages lab operations | Equipment CRUD, work orders, calibration, booking management |
| `LAB_TECHNICIAN` | Executes maintenance | View and update assigned work orders |
| `RESEARCHER` | Primary equipment user | Book equipment, view own history, access shared resources |
| `STUDENT` | Restricted researcher | Book equipment with limited scope |

---

## ⚙️ Getting Started

### Prerequisites

- **Java 17** (OpenJDK recommended)
- **Maven 3.8+**
- **Node.js 20+** and **npm 10+**
- **PostgreSQL 15** running locally or via Docker

### 1. Clone the Repository

```bash
git clone https://github.com/<your-org>/Lab_Resource_Utilization.git
cd Lab_Resource_Utilization
```

### 2. Database Setup

```sql
CREATE DATABASE "LabResourceDB";
CREATE USER postgres WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE "LabResourceDB" TO postgres;
```

### 3. Backend Setup

```bash
cd backend
# Configure environment (see Environment Configuration)
# Edit .env with your credentials

# Build and run
./mvnw spring-boot:run
```

The backend starts on **`http://localhost:8080`**.
Schema is auto-managed via `spring.jpa.hibernate.ddl-auto=update`.

### 4. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **`http://localhost:5173`**.

---

## 🌍 Environment Configuration

### Backend (`backend/.env`)

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/LabResourceDB
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your_256_bit_hex_secret_here
JWT_EXPIRATION_MS=86400000

# SMTP Email (Milestone 4 Notifications)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=noreply@yourdomain.com
SMTP_PASSWORD=your_app_password

# Application
HOST_INSTITUTION_NAME=SBMTECH University
```

### Frontend (`frontend/.env`)

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

---

## 🗂️ Project Structure

```
Lab_Resource_Utilization/
├── backend/
│   ├── src/main/java/in/sbmtechservice/Lab_Resource_Utilization/
│   │   ├── LabResourceUtilizationApplication.java   # Spring Boot entry point
│   │   ├── auth_user/                               # Milestone 1: JWT, RBAC, User
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   ├── institution/                             # Milestone 1: Institution, Department
│   │   ├── equipment_inventory/                     # Milestone 1: Equipment, Category, Tag
│   │   ├── booking_scheduling/                      # Milestone 2: Booking, Series, Waitlist
│   │   ├── resource_sharing/                        # Milestone 2: Sharing, AccessRequest
│   │   ├── maintenance_calibration/                 # Milestone 3: WorkOrder, Calibration
│   │   │   └── scheduler/
│   │   ├── cost_billing/                            # Milestone 3: Invoice, Budget, Transaction
│   │   ├── analytics_reporting/                     # Milestone 4 and 5: Analytics, Reports
│   │   ├── iot_utilization_monitoring/              # Milestone 5: Telemetry, Daily Metrics
│   │   └── notification/                            # Milestone 4: Email + In-App Notifications
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── templates/
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── api/axios.js                             # Configured Axios instance with JWT
│   │   ├── context/AuthContext.jsx                  # Global auth state
│   │   ├── components/ProtectedRoute.jsx            # Route guard
│   │   ├── pages/
│   │   │   ├── LoginPage.jsx
│   │   │   ├── SignupPage.jsx
│   │   │   └── Dashboard.jsx                        # Main SPA dashboard
│   │   ├── features/                                # Domain sub-modules
│   │   │   ├── analytics/
│   │   │   ├── auth/
│   │   │   ├── billing/
│   │   │   ├── booking/
│   │   │   ├── institution/
│   │   │   ├── inventory/
│   │   │   ├── iot/
│   │   │   ├── maintenance/
│   │   │   ├── notification/
│   │   │   └── sharing/
│   │   └── App.jsx
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
│
└── README.md
```

---

## 🤝 Contributing

1. Fork the repository.
2. Create your feature branch: `git checkout -b feature/my-new-feature`
3. Follow the existing domain-module structure for new features.
4. Write meaningful commit messages following [Conventional Commits](https://www.conventionalcommits.org/).
5. Ensure the backend compiles (`./mvnw clean compile`) and the frontend lints cleanly (`npm run lint`) before submitting a PR.
6. Open a Pull Request against `main` with a clear description of the change.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**Built with Java, React, and enterprise software engineering principles.**

*SBMTechService · Lab Resource Utilization Platform*

</div>
