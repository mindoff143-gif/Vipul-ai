package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AssistantChatMessage
import com.example.data.model.CommunicationLog
import com.example.data.model.ContactRecord
import com.example.data.model.EmailSummaryItem
import com.example.data.model.ReminderItem
import com.example.data.model.SaharanpurNewsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AssistantChatMessage::class,
        CommunicationLog::class,
        ReminderItem::class,
        ContactRecord::class,
        EmailSummaryItem::class,
        SaharanpurNewsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assistantDao(): AssistantDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_ai_assistant.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.assistantDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(dao: AssistantDao) {
            val now = System.currentTimeMillis()
            val hour = 3600 * 1000L

            // Initial Contacts
            dao.insertContact(
                ContactRecord(
                    name = "माँ (Mom)",
                    phone = "+91 98765 43210",
                    relation = "Family",
                    notes = "Always remind to take medicine"
                )
            )
            dao.insertContact(
                ContactRecord(
                    name = "Rahul Sharma",
                    phone = "+91 98123 45678",
                    relation = "Friend",
                    notes = "College friend, working in Bangalore"
                )
            )
            dao.insertContact(
                ContactRecord(
                    name = "Priya Verma",
                    phone = "+91 99887 76655",
                    relation = "Colleague",
                    notes = "Product Manager at Tech Corp"
                )
            )
            dao.insertContact(
                ContactRecord(
                    name = "Dr. Gupta Clinic",
                    phone = "+91 91234 56789",
                    relation = "Doctor",
                    notes = "Family Physician"
                )
            )
            dao.insertContact(
                ContactRecord(
                    name = "Spam Telemarketer",
                    phone = "+91 14098 76543",
                    relation = "Spam",
                    isSpamOrBlocked = true,
                    notes = "Loan and Credit Card spam calls"
                )
            )

            // Initial Reminders
            dao.insertReminder(
                ReminderItem(
                    title = "डॉक्टर से अपॉइंटमेंट और दवाई लेना",
                    timeInMillis = now + 2 * hour,
                    isLocationBased = false,
                    locationName = "City Hospital",
                    contactName = "Dr. Gupta Clinic",
                    isCompleted = false,
                    isSyncedToCalendar = true
                )
            )
            dao.insertReminder(
                ReminderItem(
                    title = "ऑफिस पहुँचते ही राहुल से प्रोजेक्ट फाइल लेना",
                    timeInMillis = now + 4 * hour,
                    isLocationBased = true,
                    locationName = "Office (Cyber City)",
                    contactName = "Rahul Sharma",
                    isCompleted = false,
                    isSyncedToCalendar = true
                )
            )
            dao.insertReminder(
                ReminderItem(
                    title = "शाम 7 बजे माँ को फोन करना",
                    timeInMillis = now + 7 * hour,
                    isLocationBased = false,
                    locationName = null,
                    contactName = "माँ (Mom)",
                    isCompleted = false,
                    isSyncedToCalendar = false
                )
            )

            // Initial Communication Memory (Call logs, SMS, WhatsApp, Telegram)
            dao.insertCommunicationLog(
                CommunicationLog(
                    contactName = "Rahul Sharma",
                    contactNumber = "+91 98123 45678",
                    platform = "WHATSAPP",
                    type = "INCOMING",
                    content = "कल शाम 5 बजे कैफ़े में मिलते हैं, प्रोजेक्ट प्रेजेंटेशन तैयार रखना।",
                    timestamp = now - 45 * 60 * 1000L,
                    aiSummary = "Rahul requested to meet at 5 PM tomorrow for presentation"
                )
            )
            dao.insertCommunicationLog(
                CommunicationLog(
                    contactName = "माँ (Mom)",
                    contactNumber = "+91 98765 43210",
                    platform = "SMS",
                    type = "INCOMING",
                    content = "बेटा, घर आते समय फल और दूध ले आना।",
                    timestamp = now - 2 * hour,
                    aiSummary = "Mom asked to bring fruits and milk on the way home"
                )
            )
            dao.insertCommunicationLog(
                CommunicationLog(
                    contactName = "Priya Verma",
                    contactNumber = "+91 99887 76655",
                    platform = "TELEGRAM",
                    type = "INCOMING",
                    content = "Q3 roadmap document has been updated on Drive. Please review before 3 PM.",
                    timestamp = now - 3 * hour,
                    aiSummary = "Priya updated Q3 roadmap for review before 3 PM"
                )
            )
            dao.insertCommunicationLog(
                CommunicationLog(
                    contactName = "Spam Telemarketer",
                    contactNumber = "+91 14098 76543",
                    platform = "CALL",
                    type = "SCREENED",
                    content = "कॉल स्क्रीनिंग: AI असिस्टेंट ने कॉल का उत्तर दिया। कॉलर ने पर्सनल लोन का ऑफर दिया था। AI ने विनम्रतापूर्वक मना कर कॉल डिस्कनेक्ट कर दी।",
                    timestamp = now - 5 * hour,
                    isSpam = true,
                    aiSummary = "Loan offer screened and declined automatically by AI",
                    callDurationSeconds = 18
                )
            )
            dao.insertCommunicationLog(
                CommunicationLog(
                    contactName = "Rahul Sharma",
                    contactNumber = "+91 98123 45678",
                    platform = "CALL",
                    type = "INCOMING",
                    content = "कॉन्फ्रेंस कॉल के बारे में बात हुई। राहुल ने बताया कि क्लाइंट ने प्रपोजल अप्रूव कर दिया है।",
                    timestamp = now - 22 * hour,
                    aiSummary = "Rahul informed client approved the proposal",
                    callDurationSeconds = 145
                )
            )

            // Initial Email Summaries
            dao.insertEmail(
                EmailSummaryItem(
                    sender = "Google Cloud Team",
                    subject = "Monthly Usage Summary & Updates",
                    snippet = "Your monthly invoice for Cloud Run and Gemini API has been generated. Total billing amount is within your budget threshold.",
                    aiSummaryHindi = "गूगल क्लाउड की मासिक रिपोर्ट: बिलिंग आपके बजट के अंदर है और सभी सेवाएं सामान्य हैं।",
                    aiSummaryEnglish = "Google Cloud report: Billing is well within budget threshold.",
                    timestamp = now - 1 * hour
                )
            )
            dao.insertEmail(
                EmailSummaryItem(
                    sender = "TechHR Conference 2026",
                    subject = "Invitation: Keynote Session on AI Assistants",
                    snippet = "Dear Delegate, we are pleased to confirm your VIP seat for the upcoming AI Innovations Summit this Friday.",
                    aiSummaryHindi = "AI सम्मिट का आमंत्रण: शुक्रवार को मुख्य सत्र के लिए आपकी VIP सीट आरक्षित कर दी गई है।",
                    aiSummaryEnglish = "AI Summit invitation: VIP seat confirmed for Friday keynote session.",
                    timestamp = now - 6 * hour
                )
            )

            // Welcome Assistant Message
            dao.insertChatMessage(
                AssistantChatMessage(
                    sender = "ASSISTANT",
                    text = "नमस्ते! मैं आपका पर्सनल AI असिस्टेंट हूँ। आप मुझसे बोलकर या लिखकर अपने कॉल्स, मैसेज, ईमेल, शेड्यूल और रिमाइंडर मैनेज करवा सकते हैं, या सहारनपुर की ताज़ा खबरें खोजकर सीधे अपने फेसबुक पेज पर पब्लिश कर सकते हैं। आज मैं आपकी क्या मदद करूँ?",
                    language = "hi",
                    actionType = "WELCOME"
                )
            )

            // Initial Saharanpur Local News
            dao.insertAllSaharanpurNews(
                listOf(
                    SaharanpurNewsEntity(
                        title = "सहारनपुर में स्मार्ट सिटी प्रोजेक्ट के तहत घंटाघर और कोर्ट रोड का होगा आधुनिक कायाकल्प",
                        summaryHindi = "सहारनपुर नगर निगम ने शहर के प्रमुख घंटाघर चौक और कोर्ट रोड के सौंदर्यीकरण हेतु नए विकास कार्य शुरू किए हैं। नए पाथवे, हेरिटेज लाइट्स और सुगम यातायात के लिए चौराहों का चौड़ीकरण किया जाएगा।",
                        category = "विकास व स्मार्ट सिटी",
                        source = "सहारनपुर अमर उजाला",
                        originalUrl = "https://news.google.com",
                        publishedDate = "आज, सुबह 10:15",
                        timestamp = now - 2 * hour,
                        formattedFbPost = """
                            🚨 #सहारनपुर_विकास_अपडेट | Saharanpur Smart City
                            ━━━━━━━━━━━━━━━━━━━━━
                            📍 स्थान: घंटाघर एवं कोर्ट रोड, सहारनपुर (उ.प्र.)

                            🏙️ सहारनपुर शहर के प्रमुख घंटाघर चौक और कोर्ट रोड का होने जा रहा है आधुनिक कायाकल्प!

                            🔹 मुख्य बिंदु:
                            • स्मार्ट सिटी मिशन के तहत हेरिटेज लाइट्स और नए पेडेस्ट्रियन पाथवे का निर्माण।
                            • चौराहों के सुंदरीकरण से जाम से मिलेगी निजात।
                            • नगर निगम सहारनपुर ने अधिकारियों को समयसीमा में कार्य पूर्ण करने के निर्देश दिए।

                            📢 इस विकास कार्य पर आपकी क्या राय है? कमेंट में ज़रूर बताएं और ऐसी ही ताज़ा खबरों के लिए पेज को फॉलो व शेयर करें!

                            #Saharanpur #SaharanpurNews #SmartCitySaharanpur #UttarPradesh #UPNews #SaharanpurSmartCity #सहारनपुर
                        """.trimIndent(),
                        isPublishedToFb = false
                    ),
                    SaharanpurNewsEntity(
                        title = "मां शाकंभरी विश्वविद्यालय सहारनपुर: नए शैक्षणिक सत्र के प्रवेश व परीक्षा फॉर्म की तिथियां जारी",
                        summaryHindi = "मां शाकंभरी विश्वविद्यालय प्रशासन ने स्नातक और परास्नातक के नए सत्र के लिए परीक्षा फॉर्म और प्रवेश काउंसलिंग का शेड्यूल जारी कर दिया है। छात्र आधिकारिक पोर्टल पर ऑनलाइन आवेदन कर सकते हैं।",
                        category = "शिक्षा व विश्वविद्यालय",
                        source = "दैनिक जागरण सहारनपुर",
                        originalUrl = "https://news.google.com",
                        publishedDate = "आज, सुबह 08:30",
                        timestamp = now - 4 * hour,
                        formattedFbPost = """
                            🎓 #सहारनपुर_शिक्षा_समाचार | Maa Shakumbhari University
                            ━━━━━━━━━━━━━━━━━━━━━
                            📍 सहारनपुर, उत्तर प्रदेश

                            📚 मां शाकंभरी विश्वविद्यालय ने नए सत्र के प्रवेश एवं परीक्षा फॉर्म का शेड्यूल किया जारी!

                            🔹 अहम जानकारियां:
                            • यूजी और पीजी के विभिन्न पाठ्यक्रमों के लिए ऑनलाइन आवेदन शुरू।
                            • छात्र यूनिवर्सिटी की ऑफिशियल वेबसाइट पर जाकर फॉर्म भर सकते हैं।
                            • छात्र हित में हेल्पलाइन नंबर और सहायता केंद्र भी सक्रिय।

                            📢 सभी छात्र साथियों के साथ यह महत्वपूर्ण जानकारी शेयर करें!

                            #MSU #MaaShakumbhariUniversity #Saharanpur #SaharanpurStudents #UPHigherEducation #सहारनपुर
                        """.trimIndent(),
                        isPublishedToFb = false
                    ),
                    SaharanpurNewsEntity(
                        title = "सहारनपुर: देवबंद और नकुड़ चीनी मिलों ने गन्ना किसानों के बकाए का रिकॉर्ड भुगतान जारी किया",
                        summaryHindi = "जिला गन्ना अधिकारी ने जानकारी दी कि सहारनपुर जनपद के गन्ना किसानों के खाते में चीनी मिलों द्वारा पिछले पेराई सत्र का बकाया भुगतान डीबीटी के माध्यम से ट्रांसफर कर दिया गया है, जिससे किसानों में खुशी की लहर है।",
                        category = "किसान व कृषि",
                        source = "हिंदुस्तान सहारनपुर",
                        originalUrl = "https://news.google.com",
                        publishedDate = "कल शाम, 06:45",
                        timestamp = now - 16 * hour,
                        formattedFbPost = """
                            🌾 #सहारनपुर_किसान_समाचार | Sugarcane Farmers News
                            ━━━━━━━━━━━━━━━━━━━━━
                            📍 देवबंद, नकुड़ व सरसावा, सहारनपुर

                            🚜 सहारनपुर के गन्ना किसानों के लिए राहत भरी खबर: चीनी मिलों ने जारी किया करोड़ों का बकाया भुगतान!

                            🔹 मुख्य अपडेट:
                            • किसानों के बैंक खातों में सीधे डीबीटी के जरिए पहुंची भुगतान राशि।
                            • आगामी पेराई सत्र की तैयारियों और तौल केंद्रों की जांच के आदेश जारी।
                            • भाकियू और किसान प्रतिनिधियों ने त्वरित भुगतान का स्वागत किया।

                            📢 जय जवान, जय किसान! इस पोस्ट को किसान भाइयों तक ज़रूर पहुँचाएं।

                            #Saharanpur #KisanNews #SugarcanePayment #Deoband #Nakur #UPAgriculture #सहारनपुर_किसान
                        """.trimIndent(),
                        isPublishedToFb = false
                    ),
                    SaharanpurNewsEntity(
                        title = "सहारनपुर का प्रसिद्ध काष्ठ कला (Wood Carving) उद्योग: अंतरराष्ट्रीय एक्सपो में मिलेगा वैश्विक मंच",
                        summaryHindi = "सहारनपुर के प्रसिद्ध लकड़ी नक्काशी उद्योग को वैश्विक स्तर पर बढ़ावा देने के लिए जिला उद्योग केंद्र द्वारा आगामी अंतरराष्ट्रीय व्यापार मेले में विशेष पवेलियन की व्यवस्था की जा रही है।",
                        category = "उद्योग व व्यापार",
                        source = "सहारनपुर टाइम्स",
                        originalUrl = "https://news.google.com",
                        publishedDate = "कल दोपहर, 01:20",
                        timestamp = now - 22 * hour,
                        formattedFbPost = """
                            🪵 #सहारनपुर_काष्ठ_कला | Saharanpur Wood Carving Global Expo
                            ━━━━━━━━━━━━━━━━━━━━━
                            📍 सहारनपुर, उत्तर प्रदेश (वुड सिटी ऑफ इंडिया)

                            ✨ सहारनपुर के विश्वप्रसिद्ध लकड़ी नक्काशी शिल्प को अंतरराष्ट्रीय स्तर पर नई पहचान दिलाने की बड़ी पहल!

                            🔹 विशेषताएं:
                            • अंतरराष्ट्रीय ट्रेड एक्सपो में सहारनपुर के कारीगरों को मिलेगा विशेष पवेलियन।
                            • ओडीओपी (ODOP) योजना के तहत कारीगरों को डिजिटल मार्केटिंग और एक्सपोर्ट सब्सिडी।
                            • हस्तशिल्पियों के चेहरे खिले, नए वैश्विक आर्डरों की उम्मीद।

                            📢 गर्व करें हमारे सहारनपुर के हुनर पर! शेयर करें और पेज को लाइक करें।

                            #Saharanpur #WoodCarving #ODOP #MadeInSaharanpur #SaharanpurArt #UPHandicrafts #सहारनपुर
                        """.trimIndent(),
                        isPublishedToFb = false
                    )
                )
            )
        }
    }
}
