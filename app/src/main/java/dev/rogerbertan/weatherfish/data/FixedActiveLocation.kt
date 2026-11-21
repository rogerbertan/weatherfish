package dev.rogerbertan.weatherfish.data

import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.Place
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FixedActiveLocation @Inject constructor() : ActiveLocation {
    override val place: Flow<Place> = flowOf(
        Place(
            name = "São Paulo",
            region = "São Paulo",
            country = "Brasil",
            point = GeoPoint(-23.5475, -46.63611),
            timeZone = ZoneId.of("America/Sao_Paulo"),
        ),
    )
}