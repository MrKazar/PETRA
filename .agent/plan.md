# Project Plan

Application Android de suivi et d'évolution sportive personnelle. Elle permet aux utilisateurs de suivre leurs séances d'entraînement, mesurer leurs progrès, visualiser leurs statistiques d'évolution au fil du temps (poids, performances, répétitions, cardio), créer des programmes d'entraînement personnalisés et suivre leurs objectifs sportifs.

## Project Brief

# Fiche Projet - Application de Suivi Sportif (MVP)

Fiche synthétique pour le développement du Produit Minimum Viable (MVP) d'une application Android de suivi et d'évolution sportive personnelle.

---

## Fonctionnalités Principales (Features)

1. **Création de programmes d'entraînement personnalisés**
   - Permet de créer, modifier et structurer des routines d'entraînement avec une liste d'exercices, le nombre de séries, les répétitions cibles et les charges ou durées.

2. **Saisie et suivi des séances en temps réel**
   - Interface dynamique permettant d'enregistrer les performances effectives pendant la séance (poids soulevé, répétitions réalisées, données cardio) et de valider chaque série.

3. **Suivi des métriques personnelles et objectifs**
   - Enregistrement régulier des mesures corporelles (ex: poids) et définition d'objectifs sportifs personnels à atteindre.

4. **Visualisation des statistiques d'évolution**
   - Graphiques et indicateurs de progression clairs pour analyser l'évolution des performances et des mesures au fil du temps.

---

## Architecture et Technologies (High-Level Tech Stack)

- **Langage** : Kotlin
- **Interface Utilisateur (UI)** : Jetpack Compose avec Material Design 3
- **Navigation & Stratégie Adaptative** :
  - **Jetpack Navigation 3** (pilotée par l'état / state-driven)
  - **Compose Material Adaptive** pour la gestion adaptative des layouts sur tous types d'écrans (smartphones, tablettes, pliables)
- **Gestion de l'État & Asynchronisme** :
  - Kotlin Coroutines & Flow
  - Architecture MVVM avec Jetpack ViewModel et `StateFlow` pour une UI réactive et cohérente

## Implementation Steps
**Total Duration:** 3m 41s

### Task_1_DataLayerAndProgramManagement: Set up Room Database schema (WorkoutProgram, Exercise, WorkoutSession, LoggedSet, BodyMetric), DAOs, Repository layer, and build UI for creating and viewing workout programs and exercises.
- **Status:** COMPLETED
- **Updates:** Room database schema, DAOs, repositories, ViewModels, ProgramListScreen, ProgramDetailScreen, ProgramFormDialog, ExerciseFormDialog, and navigation set up and verified with successful assembleDebug build.
- **Acceptance Criteria:**
  - Room DB schema and DAOs implemented
  - Repository and ViewModel for programs created
  - Program builder and routine list screens implemented in Compose
  - build pass
- **Duration:** 3m 41s

### Task_2_ActiveWorkoutSession: Implement the Active Workout Session tracking UI and ViewModel enabling real-time workout logging, set validation (weight, reps, cardio), timer/rest tracking, and saving completed sessions to Room DB.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Active workout screen with exercise set logging and validation
  - Session duration timer and weight/reps input
  - Completed sessions saved in local database
  - build pass
- **StartTime:** 2026-10-23 04:41:24 CEST

### Task_3_BodyMetricsAndProgressDashboard: Implement Body Metrics tracking (weight entry/goals) and Progress Analytics dashboard featuring evolution charts, KPIs, and personal records (PRs), integrated with bottom navigation.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Body weight logging and target tracking screen
  - Progress analytics screen displaying progress charts and personal records
  - Bottom navigation integration across all main app screens
  - build pass

### Task_4_RunAndVerify: Run and verify application stability, performance, and feature completeness. Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** PENDING
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - All core user flows verified without crashes

