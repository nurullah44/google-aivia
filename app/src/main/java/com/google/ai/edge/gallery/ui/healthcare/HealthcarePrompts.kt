/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.healthcare

import com.google.ai.edge.gallery.data.PromptTemplate

/**
 * Healthcare-specific prompt templates for medical professionals.
 * These prompts are designed to provide clinical, educational, and documentation support
 * while maintaining privacy and professional standards.
 */
object HealthcarePrompts {

    /** Prompt templates for medical image analysis */
    val MEDICAL_IMAGE_ANALYSIS_PROMPTS = listOf(
                        PromptTemplate(
                    title = "Clinical Image Description",
                    description = "Describe medical image findings for clinical documentation with patient context",
                    prompt = """You are a medical AI assistant. Analyze medical images with patient context.

Include:
- Anatomical structures and findings
- Correlation with patient history
- Clinical assessment and potential diagnoses
- Recommendations for follow-up

Use patient name when provided. For educational purposes only."""
                ),
        
        PromptTemplate(
            title = "Educational Image Review",
            description = "Create educational content from medical images",
            prompt = """Medical education AI. For images provide:
- Anatomical structures and abnormalities
- Key diagnostic features
- Differential diagnoses
- Teaching points

Educational purposes only."""
        ),
        
        PromptTemplate(
            title = "Quick Image Assessment",
            description = "Rapid preliminary assessment of medical images",
            prompt = """Medical AI for rapid assessment. For images provide:
- Primary findings
- Initial impression
- Next steps

Preliminary assessment only."""
        )
    )

    /** Prompt templates for clinical documentation */
    val CLINICAL_DOCUMENTATION_PROMPTS = listOf(
        PromptTemplate(
            title = "Clinical Note Generation",
            description = "Generate structured clinical notes from observations",
            prompt = """Based on the provided clinical information, generate a structured clinical note:

CHIEF COMPLAINT:
[Primary reason for encounter]

HISTORY OF PRESENT ILLNESS:
[Detailed symptom history and timeline]

PHYSICAL EXAMINATION:
[Relevant physical findings]

ASSESSMENT:
[Clinical assessment and differential diagnosis]

PLAN:
[Treatment plan and follow-up]

Please format this as a professional clinical note suitable for medical records. Ensure all information is accurate and properly structured."""
        ),
        
        PromptTemplate(
            title = "Discharge Summary",
            description = "Create comprehensive discharge summaries",
            prompt = """Generate a comprehensive discharge summary including:

ADMISSION DETAILS:
- Admission date and chief complaint
- Initial presentation and symptoms

HOSPITAL COURSE:
- Key events during hospitalization
- Treatments provided
- Response to interventions

DISCHARGE STATUS:
- Current condition
- Medications at discharge
- Activity restrictions

FOLLOW-UP CARE:
- Outpatient appointments
- Monitoring requirements
- Return precautions

Format as a professional discharge summary for continuity of care."""
        ),
        
        PromptTemplate(
            title = "Procedure Documentation",
            description = "Document medical procedures and interventions",
            prompt = """Document this medical procedure with the following structure:

PROCEDURE PERFORMED:
[Name and description of procedure]

INDICATION:
[Clinical reason for procedure]

TECHNIQUE:
[Step-by-step procedure description]
[Equipment used]
[Any complications or modifications]

FINDINGS:
[Key observations during procedure]

POST-PROCEDURE:
[Patient status and immediate care]
[Monitoring requirements]

PLAN:
[Follow-up care and monitoring]

Ensure documentation meets professional medical standards for procedure notes."""
        )
    )

    /** Prompt templates for patient education */
    val PATIENT_EDUCATION_PROMPTS = listOf(
        PromptTemplate(
            title = "Patient-Friendly Explanation",
            description = "Translate medical concepts into patient-friendly language",
            prompt = """Convert this medical information into patient-friendly language:

1. SIMPLE EXPLANATION:
   - Use everyday language
   - Avoid medical jargon
   - Include analogies when helpful

2. WHAT THIS MEANS FOR YOU:
   - Personal implications
   - What to expect
   - When to seek help

3. NEXT STEPS:
   - Clear action items
   - Timeline for care
   - Contact information

4. QUESTIONS TO ASK:
   - Suggest relevant questions for their healthcare team

Make this information accessible and reassuring while remaining medically accurate."""
        ),
        
        PromptTemplate(
            title = "Treatment Instructions",
            description = "Create clear treatment and medication instructions",
            prompt = """Create clear, patient-friendly treatment instructions:

MEDICATION INSTRUCTIONS:
- How to take medications
- When to take them
- What to expect
- Side effects to watch for

ACTIVITY GUIDELINES:
- What you can do
- What to avoid
- Gradual return to activities

MONITORING:
- Symptoms to watch for
- When to call the doctor
- Warning signs

LIFESTYLE RECOMMENDATIONS:
- Diet modifications
- Exercise guidelines
- Self-care tips

Use simple language and organize information clearly for easy understanding."""
        ),
        
        PromptTemplate(
            title = "Condition Overview",
            description = "Provide comprehensive condition education",
            prompt = """Provide a comprehensive but patient-friendly overview of this medical condition:

WHAT IS THIS CONDITION?
- Simple definition
- How common it is
- Who it affects

WHY DID THIS HAPPEN?
- Common causes
- Risk factors
- Prevention tips

WHAT TO EXPECT:
- Typical course
- Treatment options
- Prognosis

LIVING WITH THIS CONDITION:
- Daily management
- Lifestyle adaptations
- Support resources

Present this information in an encouraging, supportive tone while being factually accurate."""
        )
    )

    /**
     * Get appropriate prompt templates based on healthcare task type
     */
    fun getPromptsForTask(taskType: String): List<PromptTemplate> {
        return when (taskType) {
            "healthcare_image" -> MEDICAL_IMAGE_ANALYSIS_PROMPTS

        
            else -> emptyList()
        }
    }

    /**
     * Get all healthcare prompt templates
     */
    fun getAllHealthcarePrompts(): List<PromptTemplate> {
        return MEDICAL_IMAGE_ANALYSIS_PROMPTS + CLINICAL_DOCUMENTATION_PROMPTS + PATIENT_EDUCATION_PROMPTS
    }
}
