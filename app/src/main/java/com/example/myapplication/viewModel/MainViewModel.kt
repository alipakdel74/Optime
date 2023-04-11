package com.example.myapplication.viewModel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.BaseViewModel
import kotlinx.coroutines.flow.*

class MainViewModel : BaseViewModel() {

    private val _currentLocation = MutableSharedFlow<Pair<Double, Double>>()
    val currentLocation = _currentLocation.asSharedFlow()
    fun getLocation(latitude: Double, longitude: Double) {
        flow {
            emit(Pair(latitude, longitude))
        }.onEach {
            _currentLocation.emit(it)
        }.launchIn(viewModelScope)
    }
}