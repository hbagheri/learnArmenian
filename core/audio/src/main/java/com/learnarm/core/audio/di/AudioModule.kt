package com.learnarm.core.audio.di

import com.learnarm.core.audio.AudioConfig
import com.learnarm.core.audio.BuildConfig
import com.learnarm.core.audio.LetterAudioPlayer
import com.learnarm.core.audio.MediaRecorderSpeechRecorder
import com.learnarm.core.audio.PronunciationScorer
import com.learnarm.core.audio.RemotePronunciationScorer
import com.learnarm.core.audio.RemoteTtsLetterAudioPlayer
import com.learnarm.core.audio.SpeechRecorder
import com.learnarm.core.audio.defaultHttpClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    internal abstract fun bindLetterAudioPlayer(
        impl: RemoteTtsLetterAudioPlayer,
    ): LetterAudioPlayer

    @Binds
    @Singleton
    internal abstract fun bindSpeechRecorder(
        impl: MediaRecorderSpeechRecorder,
    ): SpeechRecorder

    @Binds
    @Singleton
    internal abstract fun bindPronunciationScorer(
        impl: RemotePronunciationScorer,
    ): PronunciationScorer

    companion object {

        @Provides
        @Singleton
        fun provideAudioConfig(): AudioConfig =
            AudioConfig(baseUrl = BuildConfig.LEARNARM_API_BASE_URL)

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = defaultHttpClient()
    }
}
