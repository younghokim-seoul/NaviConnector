package com.cm.naviconnector.di

import android.content.Context
import com.cm.naviconnector.feature.audio.AudioRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioRepository(@ApplicationContext context: Context): AudioRepository {
        return AudioRepository(context)
    }
}
