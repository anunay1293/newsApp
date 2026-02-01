# News Flow (News App + Serverless AWS Backend)

News Flow is an Android news application built with **Kotlin + Jetpack Compose** and backed by a **serverless AWS backend** for scheduled ingestion, storage, and low-latency reads. The goal is fast, reliable delivery of categorized news feeds with a clean, testable architecture.

---

## Tech Stack

### Android (Client)
- Kotlin, Jetpack Compose
- Retrofit
- Room Database
- Paging3
- MVVM + Repository pattern

### Backend (AWS – Serverless)
- Amazon EventBridge (scheduled ingestion)
- AWS Lambda (ingestion + read APIs)
- Amazon DynamoDB (news storage + TTL expiration)
- Amazon API Gateway (REST API layer)
- Amazon Cognito (authentication)

## Architecture

<img width="3887" height="3079" alt="image" src="https://github.com/user-attachments/assets/4b446ee5-69fa-490e-a078-d91a58aea568" />
