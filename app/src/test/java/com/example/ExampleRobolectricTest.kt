package com.example

import com.example.data.model.SampleMediaProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun testSampleMediaProviderIntegrity() {
    val videos = SampleMediaProvider.sampleVideos
    assertNotNull(videos)
    assertTrue(videos.isNotEmpty())
    assertEquals("sample_video_1", videos[0].id)

    val audios = SampleMediaProvider.sampleAudios
    assertNotNull(audios)
    assertTrue(audios.isNotEmpty())
    assertEquals("sample_audio_1", audios[0].id)
  }
}
