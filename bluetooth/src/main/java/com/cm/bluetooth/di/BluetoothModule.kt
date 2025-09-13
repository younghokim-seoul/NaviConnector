package com.cm.bluetooth.di

import android.content.Context
import com.cm.bluetooth.BluetoothClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
object BluetoothModule {

    @Provides
    @ActivityRetainedScoped
    fun provideBluetoothClient(@ApplicationContext context: Context): BluetoothClient {
        return BluetoothClient(context)
    }
}