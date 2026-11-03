package com.example.ui.strings

import com.example.engine.nlp.BengaliDateUtils

object AppStrings {

    fun isBn(lang: String): Boolean = lang != "en"

    // --- Navigation ---
    fun navAssistant(lang: String) = if (isBn(lang)) "সহকারী" else "Assistant"
    fun navAlarms(lang: String) = if (isBn(lang)) "অ্যালার্ম" else "Alarms"
    fun navSettings(lang: String) = if (isBn(lang)) "সেটিংস" else "Settings"

    // --- Home Screen ---
    fun appSubtitle(lang: String) = if (isBn(lang)) "ব্যক্তিগত ভয়েস অ্যাসিস্ট্যান্ট" else "Personal Voice Assistant"
    fun assistantActive(lang: String) = if (isBn(lang)) "সহকারী সক্রিয় (Listening)" else "Assistant Active (Listening)"
    fun assistantInactive(lang: String) = if (isBn(lang)) "সহকারী বন্ধ রয়েছে" else "Assistant Inactive (Stopped)"
    fun assistantActiveSub(lang: String) = if (isBn(lang)) "স্ক্রিন অফ বা লক অবস্থাতেও শুনছে" else "Listening even when screen is off or locked"
    fun assistantInactiveSub(lang: String) = if (isBn(lang)) "চালু করতে সুইচ অন করুন" else "Toggle switch to activate listening"

    fun statusSleeping(lang: String) = if (isBn(lang)) "সহকারী অফলাইন • ট্যাপ করে জাগিয়ে তুলুন" else "Assistant sleeping • Tap orb to wake up"
    fun statusListeningWake(lang: String) = if (isBn(lang)) "শুনছে: \"Hey Ankita\" বলুন..." else "Listening: Say \"Hey Ankita\"..."
    fun statusWakeDetected(lang: String) = if (isBn(lang)) "অঙ্কিতা সক্রিয়! নির্দেশ দিন..." else "Ankita active! Speak your command..."
    fun statusListeningCommand(lang: String) = if (isBn(lang)) "আপনার কথা শুনছি..." else "Listening to your voice..."
    fun statusProcessing(lang: String) = if (isBn(lang)) "নির্দেশ বিশ্লেষণ করা হচ্ছে..." else "Processing command..."
    fun statusSpeaking(lang: String) = if (isBn(lang)) "অঙ্কিতা উত্তর দিচ্ছে..." else "Ankita is speaking..."

    fun btnSpeakTime(lang: String) = if (isBn(lang)) "সময় বলুন" else "Speak Time"
    fun btnHeyAnkita(lang: String) = "Hey Ankita"
    fun quickCommandsTitle(lang: String) = if (isBn(lang)) "দ্রুত ভয়েস কমান্ড (ট্যাপ করুন বা মুখে বলুন)" else "Quick Voice Commands (Tap or Speak)"

    fun formatBattery(lang: String, pct: Int) = if (isBn(lang)) "${BengaliDateUtils.toBengaliDigits(pct)}%" else "$pct%"

    // --- Alarms Screen ---
    fun alarmsTitle(lang: String) = if (isBn(lang)) "অ্যালার্ম ও রিমাইন্ডার" else "Alarms & Reminders"
    fun alarmsSubtitle(lang: String) = if (isBn(lang)) "ভয়েস কমান্ড বা ম্যানুয়ালি অ্যালার্ম সেট করুন" else "Set alarms via voice command or manually"
    fun savedAlarmsBadge(lang: String, count: Int) = if (isBn(lang)) "${BengaliDateUtils.toBengaliDigits(count)} টি সংরক্ষিত" else "$count Saved"
    fun noAlarmsTitle(lang: String) = if (isBn(lang)) "কোনো অ্যালার্ম সেট করা নেই" else "No Alarms Scheduled"
    fun noAlarmsDesc(lang: String) = if (isBn(lang)) "মুখে বলুন: \"সকাল ৭টায় অ্যালার্ম দাও\"\nঅথবা নিচের '+' বাটনে ট্যাপ করুন" else "Say: \"Set alarm for 7 AM\"\nor tap the '+' button below"
    fun addAlarmTitle(lang: String) = if (isBn(lang)) "নতুন অ্যালার্ম যোগ করুন" else "Add New Alarm"
    fun alarmTitleField(lang: String) = if (isBn(lang)) "অ্যালার্মের শিরোনাম / বিবরণ" else "Alarm Title / Label"
    fun defaultAlarmTitle(lang: String) = if (isBn(lang)) "ভয়েস অ্যালার্ম" else "Voice Alarm"
    fun selectTimeLabel(lang: String) = if (isBn(lang)) "সময় নির্বাচন:" else "Select Time:"
    fun bengaliVoiceToggle(lang: String) = if (isBn(lang)) "বাংলা ভাষায় ঘোষণা দিন" else "Bengali Spoken Voice"
    fun btnSave(lang: String) = if (isBn(lang)) "সংরক্ষণ করুন" else "Save"
    fun btnCancel(lang: String) = if (isBn(lang)) "বাতিল" else "Cancel"

    // --- Settings Screen ---
    fun settingsTitle(lang: String) = if (isBn(lang)) "সেটিংস ও কনফিগারেশন" else "Settings & Configuration"
    fun settingsSubtitle(lang: String) = if (isBn(lang)) "ভয়েস, ওয়েক ওয়ার্ড এবং ব্যাটারি অপ্টিমাইজেশন পরিচালনা করুন" else "Manage voice, wake word, and battery optimization"
    fun sectionLanguage(lang: String) = if (isBn(lang)) "ভাষা নির্বাচন (Language)" else "Language Selection"
    fun sectionVoice(lang: String) = if (isBn(lang)) "কণ্ঠস্বর ও উচ্চারণ (Voice Speed & Pitch)" else "Voice Speed & Pitch"
    fun speechSpeed(lang: String) = if (isBn(lang)) "কথা বলার গতি (Speed)" else "Speech Speed"
    fun speechPitch(lang: String) = if (isBn(lang)) "কণ্ঠের সুর (Pitch)" else "Voice Pitch"
    fun btnTestVoice(lang: String) = if (isBn(lang)) "কণ্ঠস্বর পরীক্ষা করুন (Test Voice)" else "Test Voice"
    fun testVoicePhrase(lang: String) = if (isBn(lang)) "নমস্কার! আমি অঙ্কিতা, আপনার ভয়েস সহকারী। এই কণ্ঠস্বরে আমি কথা বলব।" else "Hello! I am Ankita, your personal voice assistant. This is how I speak."

    fun sectionWakeWord(lang: String) = if (isBn(lang)) "ওয়েক ওয়ার্ড কনফিগারেশন (Wake Word)" else "Wake Word Configuration"
    fun wakePhraseTitle(lang: String) = if (isBn(lang)) "ওয়েক ফ্রেজ: \"Hey Ankita\"" else "Wake Phrase: \"Hey Ankita\""
    fun wakePhraseDesc(lang: String) = if (isBn(lang)) "স্ক্রিন অফ ও লক স্ক্রিনে কাজ করে" else "Detects while screen is off or device is locked"
    fun micSensitivity(lang: String) = if (isBn(lang)) "মাইক্রোফোন সেনসিটিভিটি" else "Microphone Sensitivity"
    fun sensHigh(lang: String) = if (isBn(lang)) "উচ্চ (High)" else "High"
    fun sensLow(lang: String) = if (isBn(lang)) "শান্ত (Low)" else "Low"
    fun sensBalanced(lang: String) = if (isBn(lang)) "ভারসাম্যপূর্ণ (Balanced)" else "Balanced"
    fun screenOffTitle(lang: String) = if (isBn(lang)) "স্ক্রিন অফ ডিটেকশন (Screen-Off Wake)" else "Screen-Off Wake Detection"
    fun screenOffDesc(lang: String) = if (isBn(lang)) "ফোন লক থাকা অবস্থায় ওয়েক লক বজায় রাখবে" else "Maintains CPU partial wake lock while screen is off"

    fun sectionBattery(lang: String) = if (isBn(lang)) "ব্যাটারি অপ্টিমাইজেশন ও ব্যাকগ্রাউন্ড গাইড" else "Battery Optimization & Background Guide"
    fun batteryDesc(lang: String) = if (isBn(lang))
        "অ্যান্ড্রয়েড যাতে স্ক্রিন অফ অবস্থায় অঙ্কিতার ব্যাকগ্রাউন্ড মাইক্রোফোন সার্ভিস বন্ধ না করে, সেজন্য ব্যাটারি রেস্ট্রিকশন মুক্ত করা প্রয়োজন।"
    else
        "To prevent Android from killing Ankita's microphone service during screen-off, battery optimization restrictions must be disabled."
    fun btnBatteryExempt(lang: String, isExempt: Boolean) = if (isExempt) {
        if (isBn(lang)) "ব্যাটারি অপ্টিমাইজেশন মুক্ত করা আছে ✓" else "Battery Optimization Disabled ✓"
    } else {
        if (isBn(lang)) "ব্যাটারি অপ্টিমাইজেশন বন্ধ করুন (Ignore Battery)" else "Disable Battery Optimization"
    }

    fun vivoGuideTitle(lang: String) = if (isBn(lang)) "Vivo (FuntouchOS / OriginOS) বিশেষ নির্দেশনা:" else "Vivo (FuntouchOS / OriginOS) Special Guide:"
    fun vivoGuideSteps(lang: String) = if (isBn(lang))
        "১. Settings > Battery > High Background Power Consumption-এ যান এবং 'Ankita' সক্রিয় করুন।\n২. Settings > Apps > Autostart-এ গিয়ে 'Ankita' চালু করুন।\n৩. Recent Apps স্ক্রিনে গিয়ে অঙ্কিতা অ্যাপটিতে 'Lock' আইকনে চাপ দিন।"
    else
        "1. Go to Settings > Battery > High Background Power Consumption > Enable 'Ankita'.\n2. Go to Settings > Apps > Autostart > Enable 'Ankita'.\n3. Lock Ankita in the recent apps multitasking tray."

    fun sectionAi(lang: String) = if (isBn(lang)) "AI কথোপকথন ও ক্লাউড মডিউল" else "AI Conversation & Cloud Module"
    fun aiTitle(lang: String) = if (isBn(lang)) "উন্নত AI কথোপকথন" else "Advanced AI Conversation"
    fun aiDesc(lang: String) = if (isBn(lang)) "অফলাইন ইউটিলিটি কমান্ড ব্যতীত সাধারণ প্রশ্নোত্তরের জন্য" else "For general conversational Q&A beyond offline utility commands"
    fun aiSecurityNote(lang: String) = if (isBn(lang))
        "🔒 নিরাপত্তা তথ্য: অঙ্কিতার সময়, তারিখ, অ্যালার্ম ও ব্যাটারি কমান্ড ১০০% অফলাইনে ডিভাইসের মধ্যে সম্পন্ন হয়। এপিআই কি সরাসরি অ্যাপ্লিকেশনে হার্ডকোড করা নিষিদ্ধ। ব্যাকএন্ড ক্লাউড ফাংশন বা ফায়ারবেস এআই লজিকের মাধ্যমে নিরাপদ এনভায়রনমেন্টে কি যুক্ত করুন।"
    else
        "🔒 Security Note: Time, date, day, alarms, and battery commands run 100% offline locally on-device. API keys must never be hardcoded into client code. Connect through a secure cloud backend or Firebase AI Logic."

    fun sectionPrivacy(lang: String) = if (isBn(lang)) "গোপনীয়তা ও মাইক্রোফোন নিয়ন্ত্রণ" else "Privacy & Microphone Controls"
    fun privacyDesc(lang: String) = if (isBn(lang))
        "মাইক্রোফোন শুধুমাত্র ওয়েক ওয়ার্ড খোঁজার জন্য লোকাল মেমরিতে অডিও ফ্রেম বিশ্লেষণ করে। কোনো অডিও বাইরে পাঠানো হয় না। সার্ভিস বন্ধ থাকা অবস্থায় কোনো অডিও প্রসেসিং হয় না।"
    else
        "The microphone strictly analyzes audio frames in local memory for the wake pattern. Zero audio is stored or transmitted externally. When the service is stopped, no audio processing occurs."
    fun btnOpenSettings(lang: String) = if (isBn(lang)) "অ্যাপ পারমিশন সেটিংস খুলুন" else "Open App Permission Settings"
}
