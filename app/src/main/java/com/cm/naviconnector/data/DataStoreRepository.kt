package com.cm.naviconnector.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cm.naviconnector.feature.control.Feature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class DataStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun getKey(feature: Feature): Preferences.Key<Int> {
        return intPreferencesKey(feature.id)
    }

    suspend fun saveFeatureLevel(feature: Feature, level: Int) {
        context.dataStore.edit { settings ->
            settings[getKey(feature)] = level
        }
    }

    fun getFeatureLevel(feature: Feature): Flow<Int?> {
        return context.dataStore.data.map { preferences ->
            preferences[getKey(feature)]
        }
    }
}
