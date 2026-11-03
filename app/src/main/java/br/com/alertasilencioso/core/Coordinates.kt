package br.com.alertasilencioso.core

/**
 * Lê coordenadas coladas pelo usuário: "-19.4683, -42.5367" (como o Google Maps mostra), um link do
 * Google Maps, ou o formato brasileiro com vírgula decimal ("-19,4683; -42,5367").
 */
object Coordinates {
    private val DOT_DECIMAL = Regex("""(-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""")
    private val COMMA_DECIMAL = Regex("""(-?\d{1,2},\d+)\s*[;\s]\s*(-?\d{1,3},\d+)""")

    fun parse(text: String): GeoPoint? {
        val match = DOT_DECIMAL.find(text) ?: COMMA_DECIMAL.find(text) ?: return null
        val lat = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return GeoPoint(lat, lng)
    }
}
