package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Personal AI Assistant", appName)
  }

  @Test
  fun `test gemini fallback engine handles schedule query in Hindi`() {
    val result = com.example.data.gemini.GeminiFallbackEngine.processQuery("आज का क्या शेड्यूल है?", "", "hi")
    assertEquals("SCHEDULE_BRIEFING", result.actionType)
    org.junit.Assert.assertTrue(result.responseText.contains("शेड्यूल") || result.responseText.contains("मीटिंग"))
  }

  @Test
  fun `test call screener detects telemarketing spam`() {
    val screener = com.example.service.CallScreenerManager(ApplicationProvider.getApplicationContext())
    val decision = screener.screenIncomingCall("+911409876543")
    org.junit.Assert.assertTrue(decision.isSpam)
    assertEquals("AUTO_REJECTED", decision.actionTaken)
  }

  @Test
  fun `test gemini fallback engine handles saharanpur news search query`() {
    val result = com.example.data.gemini.GeminiFallbackEngine.processQuery("सहारनपुर की ताज़ा खबरें खोजो", "", "hi")
    assertEquals("SEARCH_SAHARANPUR_NEWS", result.actionType)
    org.junit.Assert.assertTrue(result.responseText.contains("सहारनपुर"))
  }

  @Test
  fun `test gemini fallback engine handles saharanpur news facebook publish query`() {
    val result = com.example.data.gemini.GeminiFallbackEngine.processQuery("सहारनपुर न्यूज़ फेसबुक पेज पर पब्लिश कर दो", "", "hi")
    assertEquals("PUBLISH_SAHARANPUR_NEWS_FB", result.actionType)
    org.junit.Assert.assertTrue(result.responseText.contains("फेसबुक"))
  }

  @Test
  fun `test facebook post formatting contains hashtags and hindi content`() {
    val post = com.example.service.SaharanpurNewsService.generateFacebookPost(
        title = "सहारनपुर में स्मार्ट सिटी विकास कार्य तेज",
        summary = "घंटाघर व मुख्य मार्गों का सुंदरीकरण किया जा रहा है।",
        category = "विकास व स्मार्ट सिटी",
        source = "सहारनपुर लाइव",
        url = "https://news.google.com"
    )
    org.junit.Assert.assertTrue(post.contains("#Saharanpur"))
    org.junit.Assert.assertTrue(post.contains("सहारनपुर"))
    org.junit.Assert.assertTrue(post.contains("उत्तर प्रदेश"))
  }
}
