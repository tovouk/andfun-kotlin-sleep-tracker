/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    private var tonight = MutableLiveData<SleepNight?>()
    private val nights = database.getAllNights()
    val nightString = Transformations.map(nights) {
        formatNights(nights.value!!,application.resources)
    }

    private var _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    val startButtonVisible = Transformations.map(tonight) {
        null == it
    }
    val stopButtonVisible = Transformations.map(tonight) {
        null != it
    }
    val clearButtonVisible = Transformations.map(nights) {
        it?.isNotEmpty()
    }

    private var _showSnackbarEvent = MutableLiveData<Boolean>()
    val showSnackBarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        viewModelScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun getTonightFromDatabase() : SleepNight? {
        var night = database.getTonight()
        if(night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }

    fun onStartTracking(){
        viewModelScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(night: SleepNight) {
        database.insert(night)
    }

    fun onStopTracking(){
        viewModelScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            _navigateToSleepQuality.value = oldNight
            update(oldNight)

        }
    }

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    private suspend fun update(night: SleepNight){
        database.update(night)
    }

    fun onClear(){
        viewModelScope.launch {
            clear()
            tonight.value = null
            _showSnackbarEvent.value = true
        }
    }

    suspend fun clear(){
        database.clear()
    }

    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = false
    }

}

