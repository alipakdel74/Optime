package com.example.myapplication.ui.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.example.myapplication.ext.toIsoDate
import com.example.myapplication.model.Location
import com.example.myapplication.viewModel.MainViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.compose.koinViewModel
import java.util.*

@Composable
fun LocationList(vm: MainViewModel = koinViewModel()) {
    val locationList = remember { mutableStateListOf<Location>() }
    LaunchedEffect(Unit) {
        vm.currentLocation.onEach {
            locationList.add(Location(it.first, it.second, Date()))
        }.collect()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        repeat(locationList.size) { index ->
            LocationItemView(locationList[index])
            if (index < locationList.size - 1)
                Divider(modifier = Modifier.padding(8.dp))
        }
    }

}

@Composable
fun LocationItemView(data: Location) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "${data.lat} - ${data.long}", fontSize = TextUnit(14f, TextUnitType.Sp))
        Text(text = data.date.toIsoDate(), fontSize = TextUnit(12f, TextUnitType.Sp))
    }
}