package com.example.data.model

/**
 * Provides rich sample media files for immediate testing and verification
 * on emulators or devices without local media libraries.
 */
object SampleMediaProvider {

    val sampleVideos: List<MediaItemModel> = listOf(
        MediaItemModel(
            id = "sample_video_1",
            title = "Big Buck Bunny (1080p 60fps)",
            artist = "Blender Open Movie Project",
            album = "Open Source Cinema",
            durationMs = 596000L, // ~9m56s
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            mimeType = "video/mp4",
            sizeBytes = 158000000L,
            dateAdded = System.currentTimeMillis() - 86400000L,
            folderName = "Movies",
            width = 1920,
            height = 1080,
            mediaType = MediaType.VIDEO,
            isSample = true
        ),
        MediaItemModel(
            id = "sample_video_2",
            title = "Tears of Steel (4K Sci-Fi)",
            artist = "Mango Open Movie Project",
            album = "Sci-Fi Short Films",
            durationMs = 734000L, // ~12m14s
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            mimeType = "video/mp4",
            sizeBytes = 240000000L,
            dateAdded = System.currentTimeMillis() - 43200000L,
            folderName = "Movies",
            width = 3840,
            height = 2160,
            mediaType = MediaType.VIDEO,
            isSample = true
        ),
        MediaItemModel(
            id = "sample_video_3",
            title = "Sintel Animated Epic",
            artist = "Durian Open Movie Project",
            album = "Fantasy Short Films",
            durationMs = 888000L, // ~14m48s
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            mimeType = "video/mp4",
            sizeBytes = 180000000L,
            dateAdded = System.currentTimeMillis() - 172800000L,
            folderName = "Animations",
            width = 1920,
            height = 818,
            mediaType = MediaType.VIDEO,
            isSample = true
        ),
        MediaItemModel(
            id = "sample_video_4",
            title = "Elephants Dream (CGI Pioneer)",
            artist = "Orange Open Movie Project",
            album = "Classic Animation",
            durationMs = 653000L,
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            mimeType = "video/mp4",
            sizeBytes = 120000000L,
            dateAdded = System.currentTimeMillis() - 259200000L,
            folderName = "Animations",
            width = 1920,
            height = 1080,
            mediaType = MediaType.VIDEO,
            isSample = true
        ),
        MediaItemModel(
            id = "sample_video_5",
            title = "For Bigger Blazes (Action Demo)",
            artist = "Google TV Samples",
            album = "Chromecast Showcases",
            durationMs = 15000L,
            uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            mimeType = "video/mp4",
            sizeBytes = 15000000L,
            dateAdded = System.currentTimeMillis() - 500000L,
            folderName = "Showcase",
            width = 1280,
            height = 720,
            mediaType = MediaType.VIDEO,
            isSample = true
        )
    )

    val sampleAudios: List<MediaItemModel> = listOf(
        MediaItemModel(
            id = "sample_audio_1",
            title = "Synthwave Odyssey",
            artist = "Cyber Pulse",
            album = "Neon Horizons 2088",
            durationMs = 214000L, // 3:34
            uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = 8500000L,
            dateAdded = System.currentTimeMillis() - 3600000L,
            folderName = "Music/Electronic",
            mediaType = MediaType.AUDIO,
            isSample = true
        ),
        MediaItemModel(
            id = "sample_audio_2",
            title = "Lo-Fi Midnight Breeze",
            artist = "Aura Bloom",
            album = "Chillhop Study Sessions",
            durationMs = 188000L, // 3:08
            uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = 7200000L,
            dateAdded = System.currentTimeMillis() - 7200000L,
            folderName = "Music/Lo-Fi",
            mediaType = MediaType.AUDIO,
            isSample = true
        ),
        MediaItemModel(
            id = "sample_audio_3",
            title = "Acoustic Sunrise",
            artist = "Elena Vance",
            album = "Morning Whispers",
            durationMs = 245000L, // 4:05
            uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = 9400000L,
            dateAdded = System.currentTimeMillis() - 14400000L,
            folderName = "Music/Acoustic",
            mediaType = MediaType.AUDIO,
            isSample = true
        ),
        MediaItemModel(
            id = "sample_audio_4",
            title = "Deep Electro Pulse",
            artist = "SubZero Project",
            album = "Bass Protocol",
            durationMs = 195000L, // 3:15
            uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = 7800000L,
            dateAdded = System.currentTimeMillis() - 28800000L,
            folderName = "Music/Electronic",
            mediaType = MediaType.AUDIO,
            isSample = true
        )
    )

    val sampleLyrics: Map<String, List<Pair<Long, String>>> = mapOf(
        "sample_audio_1" to listOf(
            0L to "♪ Synthesizer intro fading in ♪",
            12000L to "Driving through the neon city lights",
            24000L to "Reflections on the windshield shining bright",
            36000L to "Through the digital grid we find our way",
            48000L to "Chasing tomorrow, living for today",
            62000L to "♪ Bass drops into synthwave solo ♪",
            90000L to "Electric pulses through the midnight air",
            104000L to "Lost in the rhythm without a single care",
            120000L to "Neon horizons calling out your name",
            140000L to "Nothing will ever be the same",
            165000L to "♪ Outro fading gently ♪"
        ),
        "sample_audio_2" to listOf(
            0L to "♪ Rain falling softly outside the window ♪",
            10000L to "Cup of warm coffee, steam in the air",
            22000L to "Turning pages of memories everywhere",
            38000L to "Gentle vinyl crackle on the turntable",
            54000L to "Writing stories that we know are fable",
            75000L to "Midnight breeze across the room",
            95000L to "Chasing shadows, watching the moon",
            120000L to "♪ Gentle jazz Rhodes piano solo ♪",
            150000L to "Rest easy now until the morning sun"
        )
    )
}
