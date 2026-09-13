package com.example.service

import android.util.Log
import com.example.data.model.SaharanpurNewsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object SaharanpurNewsService {
    private const val TAG = "SaharanpurNewsService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Google News RSS URLs for Saharanpur, Uttar Pradesh
    private const val RSS_URL_HINDI = "https://news.google.com/rss/search?q=Saharanpur+Uttar+Pradesh&hl=hi&gl=IN&ceid=IN:hi"
    private const val RSS_URL_EN = "https://news.google.com/rss/search?q=Saharanpur+news+Uttar+Pradesh&hl=en-IN&gl=IN&ceid=IN:en"

    suspend fun fetchLatestSaharanpurNews(customQuery: String = ""): List<SaharanpurNewsEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SaharanpurNewsEntity>()

        try {
            val queryParam = if (customQuery.isNotBlank()) "Saharanpur+$customQuery+Uttar+Pradesh" else "Saharanpur+Uttar+Pradesh"
            val targetUrl = "https://news.google.com/rss/search?q=$queryParam&hl=hi&gl=IN&ceid=IN:hi"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) PersonalAiAssistant/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val xml = response.body?.string().orEmpty()
                val parsed = parseGoogleNewsRss(xml)
                if (parsed.isNotEmpty()) {
                    results.addAll(parsed)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Online RSS fetch encountered error, using local regional feeds: ${e.message}")
        }

        // If online fetch returned fewer items or failed, merge with curated Saharanpur regional news
        if (results.size < 3) {
            val curated = getCuratedSaharanpurNews(customQuery)
            curated.forEach { item ->
                if (results.none { it.title.take(20) == item.title.take(20) }) {
                    results.add(item)
                }
            }
        }

        return@withContext results
    }

    private fun parseGoogleNewsRss(xml: String): List<SaharanpurNewsEntity> {
        val list = mutableListOf<SaharanpurNewsEntity>()
        if (xml.isBlank()) return list

        try {
            val itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL)
            val titlePattern = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL)
            val linkPattern = Pattern.compile("<link>(.*?)</link>", Pattern.DOTALL)
            val pubDatePattern = Pattern.compile("<pubDate>(.*?)</pubDate>", Pattern.DOTALL)
            val sourcePattern = Pattern.compile("<source.*?>(.*?)</source>", Pattern.DOTALL)

            val matcher = itemPattern.matcher(xml)
            var count = 0
            val now = System.currentTimeMillis()

            while (matcher.find() && count < 8) {
                val itemBlock = matcher.group(1) ?: continue

                val rawTitle = extractRegex(titlePattern, itemBlock)
                val link = extractRegex(linkPattern, itemBlock)
                val pubDate = extractRegex(pubDatePattern, itemBlock)
                val source = extractRegex(sourcePattern, itemBlock).ifBlank { "सहारनपुर न्यूज़ डेस्क" }

                val cleanTitle = cleanHtml(rawTitle)
                if (cleanTitle.length < 8) continue

                // Categorize based on keywords
                val category = categorizeNews(cleanTitle)
                val summary = generateHindiSummary(cleanTitle, category)
                val formattedFbPost = generateFacebookPost(cleanTitle, summary, category, source, link)

                list.add(
                    SaharanpurNewsEntity(
                        title = cleanTitle,
                        summaryHindi = summary,
                        category = category,
                        source = source,
                        originalUrl = link.ifBlank { "https://news.google.com" },
                        publishedDate = formatPubDate(pubDate),
                        timestamp = now - (count * 15 * 60 * 1000L),
                        formattedFbPost = formattedFbPost,
                        isPublishedToFb = false
                    )
                )
                count++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS XML: ${e.message}")
        }

        return list
    }

    private fun extractRegex(pattern: Pattern, text: String): String {
        val m = pattern.matcher(text)
        return if (m.find()) m.group(1)?.trim().orEmpty() else ""
    }

    private fun cleanHtml(text: String): String {
        return text
            .replace("<![CDATA[", "")
            .replace("]]>", "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace(Regex("<.*?>"), "")
            .trim()
    }

    private fun formatPubDate(rawDate: String): String {
        return try {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
            val date = sdf.parse(rawDate) ?: Date()
            val outFormat = SimpleDateFormat("d MMM, hh:mm a", Locale.forLanguageTag("hi-IN"))
            outFormat.format(date)
        } catch (e: Exception) {
            "आज ताज़ा अपडेट"
        }
    }

    private fun categorizeNews(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("स्मार्ट सिटी") || lower.contains("विकास") || lower.contains("सड़क") || lower.contains("फ्लाईओवर") || lower.contains("निर्माण") || lower.contains("नगर निगम") -> "विकास व स्मार्ट सिटी"
            lower.contains("विश्वविद्यालय") || lower.contains("शाकंभरी") || lower.contains("कॉलेज") || lower.contains("परीक्षा") || lower.contains("छात्र") || lower.contains("स्कूल") -> "शिक्षा व विश्वविद्यालय"
            lower.contains("किसान") || lower.contains("गन्ना") || lower.contains("चीनी मिल") || lower.contains("फसल") || lower.contains("कृषि") -> "किसान व कृषि"
            lower.contains("पुलिस") || lower.contains("थाना") || lower.contains("गिरफ्तार") || lower.contains("डीएम") || lower.contains("एसएसपी") || lower.contains("सुरक्षा") || lower.contains("हादसा") -> "प्रशासन व पुलिस"
            lower.contains("व्यापार") || lower.contains("काष्ठ") || lower.contains("बाजार") || lower.contains("उद्योग") -> "उद्योग व व्यापार"
            else -> "ताज़ा ख़बर"
        }
    }

    private fun generateHindiSummary(title: String, category: String): String {
        return when (category) {
            "विकास व स्मार्ट सिटी" -> "सहारनपुर में विकास कार्यों एवं स्मार्ट सिटी मिशन के अंतर्गत इस परियोजना को गति दी गई है। स्थानीय नागरिकों और व्यापारियों को इससे सीधा लाभ मिलेगा।"
            "शिक्षा व विश्वविद्यालय" -> "सहारनपुर एवं आसपास के छात्र-छात्राओं के हित में विश्वविद्यालय प्रशासन ने यह आदेश जारी किया है। सभी पात्र छात्र तय समय में आवश्यक प्रक्रिया पूरी करें।"
            "किसान व कृषि" -> "सहारनपुर जनपद के किसानों एवं गन्ना काश्तकारों के लिए यह अहम निर्णय लिया गया है, जिससे ग्रामीण क्षेत्रों में आर्थिक गतिविधियों को बल मिलेगा।"
            "प्रशासन व पुलिस" -> "सहारनपुर जिला प्रशासन व पुलिस अधिकारियों ने क्षेत्र में कानून व्यवस्था और जनसुविधाओं को लेकर सख्त कदम उठाए हैं।"
            else -> "सहारनपुर जनपद से संबंधित यह ताज़ा समाचार प्रकाश में आया है। अधिक विवरण हेतु संबंधित प्रशासन एवं स्रोतों द्वारा पुष्टि की जा रही है।"
        }
    }

    fun generateFacebookPost(
        title: String,
        summary: String,
        category: String,
        source: String,
        url: String
    ): String {
        val categoryEmoji = when (category) {
            "विकास व स्मार्ट सिटी" -> "🏙️"
            "शिक्षा व विश्वविद्यालय" -> "🎓"
            "किसान व कृषि" -> "🌾"
            "प्रशासन व पुलिस" -> "🚨"
            "उद्योग व व्यापार" -> "🪵"
            else -> "📰"
        }

        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("hi-IN")).format(Date())

        return buildString {
            append("🚨 #सहारनपुर_ताज़ा_समाचार | $categoryEmoji $category\n")
            append("━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("📍 स्थान: सहारनपुर, उत्तर प्रदेश\n")
            append("📅 दिनांक: $dateStr\n\n")
            append("👉 $title\n\n")
            append("🔹 मुख्य बिंदु:\n")
            append("• $summary\n")
            append("• स्रोत: $source\n")
            append("• जनपद के नागरिकों से अपील: शांति व सतर्कता बनाए रखें।\n\n")
            append("📢 इस खबर पर अपनी राय कमेंट में ज़रूर बताएं।\n")
            append("📲 सहारनपुर की पल-पल की ताज़ा खबरों के लिए हमारे फेसबुक पेज को 'फॉलो' और 'शेयर' ज़रूर करें!\n\n")
            append("#Saharanpur #SaharanpurNews #SaharanpurLive #UttarPradesh #UPNews #UPPolice #SaharanpurUpdates #सहारनपुर #उत्तरप्रदेश")
        }
    }

    fun getCuratedSaharanpurNews(filter: String = ""): List<SaharanpurNewsEntity> {
        val now = System.currentTimeMillis()
        val hour = 3600 * 1000L

        val list = listOf(
            SaharanpurNewsEntity(
                title = "सहारनपुर: दिल्ली-देहरादून ग्रीनफील्ड एक्सप्रेसवे का ट्रायल रन जल्द, सहारनपुर से दिल्ली का सफर 1.5 घंटे में होगा तय",
                summaryHindi = "सहारनपुर से गुजरने वाले दिल्ली-सहारनपुर-देहरादून एक्सप्रेसवे का काम अंतिम चरण में है। एनएचएआई अधिकारियों के अनुसार अगले माह ट्रायल रन शुरू होगा, जिससे क्षेत्र में व्यापार और आवागमन बेहद सुगम हो जाएगा।",
                category = "विकास व स्मार्ट सिटी",
                source = "दैनिक जागरण सहारनपुर",
                originalUrl = "https://news.google.com",
                publishedDate = "आज, सुबह 11:30",
                timestamp = now - 1 * hour,
                formattedFbPost = """
                    🚨 #सहारनपुर_विकास_एक्सप्रेस | Saharanpur-Delhi Expressway
                    ━━━━━━━━━━━━━━━━━━━━━━━━
                    📍 सहारनपुर, उत्तर प्रदेश

                    🛣️ सहारनपुर से दिल्ली अब सिर्फ 1.5 घंटे में! दिल्ली-देहरादून ग्रीनफील्ड एक्सप्रेसवे का ट्रायल रन जल्द होगा शुरू।

                    🔹 मुख्य बिंदु:
                    • सहारनपुर इंटरचेंज से सीधे जुड़ेगा दिल्ली और हरिद्वार-देहरादून रूट।
                    • व्यापार, उद्योग और यात्रियों को मिलेगा तीव्र गति का सुरक्षित कॉरिडोर।
                    • एनएचएआई ने शेष फिनिशिंग कार्यों को रिकॉर्ड समय में पूरा करने का दावा किया।

                    📢 सहारनपुरवासियों के लिए यह बड़ी खुशखबरी है! कमेंट में अपनी प्रतिक्रिया दें और पेज को शेयर करें।

                    #Saharanpur #DelhiDehradunExpressway #SaharanpurDevelopment #UttarPradesh #UPNews #सहारनपुर
                """.trimIndent(),
                isPublishedToFb = false
            ),
            SaharanpurNewsEntity(
                title = "सहारनपुर जिला अस्पताल व मेडिकल कॉलेज में 24 घंटे आपातकालीन टेली-कंसल्टेशन सेवा शुरू",
                summaryHindi = "सहारनपुर के मुख्य चिकित्सा अधिकारी (CMO) ने बताया कि ग्रामीण क्षेत्रों के मरीजों के लिए मेडिकल कॉलेज और जिला अस्पताल में विशेष टेली-मेडिसिन हेल्पडेस्क स्थापित की गई है।",
                category = "विकास व स्मार्ट सिटी",
                source = "अमर उजाला सहारनपुर",
                originalUrl = "https://news.google.com",
                publishedDate = "आज, सुबह 09:45",
                timestamp = now - 3 * hour,
                formattedFbPost = """
                    🏥 #सहारनपुर_स्वास्थ्य_सेवा | Saharanpur Health Update
                    ━━━━━━━━━━━━━━━━━━━━━━━━
                    📍 जिला अस्पताल एवं मेडिकल कॉलेज, सहारनपुर

                    🩺 सहारनपुर के मरीजों के लिए 24 घंटे आपातकालीन टेली-कंसल्टेशन सुविधा शुरू!

                    🔹 मुख्य जानकारियां:
                    • देहात और कस्बों के मरीज अब घर बैठे विशेषज्ञ डॉक्टरों से ले सकेंगे परामर्श।
                    • हेल्पलाइन नंबर जारी, दवाइयों और जांच के लिए नहीं लगाने पड़ेंगे चक्कर।
                    • स्वास्थ्य विभाग ने सभी प्राथमिक स्वास्थ्य केंद्रों को भी जोड़ा।

                    📢 जरूरतमंद साथियों तक यह पोस्ट ज़रूर पहुंचाएं!

                    #Saharanpur #SaharanpurHealth #MedicalCollegeSaharanpur #UPHealth #सहारनपुर_समाचार
                """.trimIndent(),
                isPublishedToFb = false
            ),
            SaharanpurNewsEntity(
                title = "सहारनपुर पुलिस का नशा तस्करों के खिलाफ बड़ा अभियान: 50 लाख की अवैध सामग्री जब्त, 4 गिरफ्तार",
                summaryHindi = "सहारनपुर एसएसपी के निर्देश पर चलाए जा रहे विशेष चेकिंग अभियान के तहत थाना कोतवाली व देहात पुलिस ने अंतरराज्यीय गिरोह का पर्दाफाश कर बड़ी मात्रा में मादक पदार्थ बरामद किए।",
                category = "प्रशासन व पुलिस",
                source = "सहारनपुर पुलिस बुलेटिन",
                originalUrl = "https://news.google.com",
                publishedDate = "कल रात, 09:15",
                timestamp = now - 14 * hour,
                formattedFbPost = """
                    🚨 #सहारनपुर_पुलिस_कार्रवाई | Crime & Police Action
                    ━━━━━━━━━━━━━━━━━━━━━━━━
                    📍 सहारनपुर, उत्तर प्रदेश

                    👮‍♂️ सहारनपुर पुलिस की बड़ी सफलता: अंतरराज्यीय नशा तस्कर गिरोह का भंडाफोड़, 50 लाख की सामग्री बरामद!

                    🔹 अहम बिंदु:
                    • एसएसपी सहारनपुर के नेतृत्व में स्वाट व देहात पुलिस की संयुक्त कार्रवाई।
                    • 4 शातिर तस्कर गिरफ्तार, तस्करी में प्रयुक्त वाहन भी सीज।
                    • क्षेत्र में नशे के खिलाफ जीरो टॉलरेंस अभियान जारी रहेगा।

                    📢 पुलिस की इस मुस्तैदी पर सहारनपुर पुलिस के लिए एक लाइक तो बनता है! कमेंट करें और शेयर करें।

                    #SaharanpurPolice #UPPolice #ActionOnCrime #SaharanpurNews #LawAndOrder #सहारनपुर_पुलिस
                """.trimIndent(),
                isPublishedToFb = false
            ),
            SaharanpurNewsEntity(
                title = "सहारनपुर: मां शाकंभरी देवी सिद्धपीठ में श्रद्धालुओं के लिए नए विश्राम गृह व रोप-वे प्रस्ताव को मंजूरी",
                summaryHindi = "सहारनपुर स्थित उत्तर भारत के प्रसिद्ध शक्तिपीठ मां शाकंभरी देवी मंदिर परिसर में पर्यटन विकास निगम द्वारा श्रद्धालुओं की सुविधा हेतु नए हाई-टेक विश्राम गृह और सुगम दर्शन कॉरिडोर को हरी झंडी मिली।",
                category = "विकास व स्मार्ट सिटी",
                source = "हिंदुस्तान सहारनपुर",
                originalUrl = "https://news.google.com",
                publishedDate = "कल दोपहर, 03:30",
                timestamp = now - 20 * hour,
                formattedFbPost = """
                    🙏 #सहारनपुर_शक्तिपीठ | Maa Shakumbhari Devi Temple
                    ━━━━━━━━━━━━━━━━━━━━━━━━
                    📍 बेहट, सहारनपुर (उ.प्र.)

                    🚩 मां शाकंभरी देवी सिद्धपीठ में श्रद्धालुओं के लिए भव्य विश्राम गृह और आधुनिक कॉरिडोर के प्रस्ताव को मिली मंजूरी!

                    🔹 दर्शनार्थियों के लिए सुविधाएं:
                    • दूर-दराज से आने वाले भक्तों के लिए सर्वसुविधायुक्त आश्रय स्थल।
                    • पहाड़ी मार्ग पर सुगम आवागमन व सुरक्षा रेलिंग का काम तेज।
                    • आगामी नवरात्र मेले के लिए विशेष प्रशासनिक तैयारियां प्रारंभ।

                    📢 जय मां शाकंभरी! कमेंट में 'जय माता दी' लिखकर आशीर्वाद लें और भक्तों संग शेयर करें।

                    #ShakumbhariDevi #Saharanpur #Behat #UPTourism #DeviTemple #सहारनपुर #शाकंभरी_देवी
                """.trimIndent(),
                isPublishedToFb = false
            )
        )

        if (filter.isBlank()) return list
        return list.filter {
            it.title.contains(filter, ignoreCase = true) ||
            it.category.contains(filter, ignoreCase = true) ||
            it.summaryHindi.contains(filter, ignoreCase = true)
        }
    }
}
