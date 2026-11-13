package com.vknxworks.sumctrl.data.convert

import androidx.annotation.StringRes
import com.vknxworks.sumctrl.R
import java.math.BigDecimal
import java.math.MathContext

private val MC = MathContext.DECIMAL128

/**
 * A unit: how to get from it to its category's base unit and back. Most are a plain factor (exact decimal
 * maths); temperature and fuel economy need their own formulas.
 */
class ConvUnit(
    val id: String,
    @StringRes val name: Int,
    /** The short symbol: "km", "°C", "GiB". Not translated. */
    val symbol: String,
    val toBase: (BigDecimal) -> BigDecimal,
    val fromBase: (BigDecimal) -> BigDecimal,
) {
    companion object {
        fun factor(id: String, @StringRes name: Int, symbol: String, factor: String): ConvUnit {
            val f = BigDecimal(factor)
            return ConvUnit(id, name, symbol, { it.multiply(f, MC) }, { it.divide(f, MC) })
        }
    }
}

class Category(val id: String, @StringRes val name: Int, val units: List<ConvUnit>, val defaultFrom: String, val defaultTo: String) {
    fun unit(id: String?): ConvUnit? = units.firstOrNull { it.id == id }

    /** [value] in [from] converted to [to]; null when there's no answer (0 km/L as L/100 km). */
    fun convert(value: BigDecimal, from: ConvUnit, to: ConvUnit): BigDecimal? = try {
        if (from === to) value else to.fromBase(from.toBase(value))
    } catch (e: ArithmeticException) {
        null
    }
}

private fun f(id: String, @StringRes name: Int, symbol: String, factor: String) = ConvUnit.factor(id, name, symbol, factor)

/** Every category. Factors are exact where a definition exists (inch = 0.0254 m, lb = 0.45359237 kg...). */
object Units {
    private val length = Category(
        "length", R.string.cat_length,
        listOf(
            f("mm", R.string.u_millimetre, "mm", "0.001"),
            f("cm", R.string.u_centimetre, "cm", "0.01"),
            f("m", R.string.u_metre, "m", "1"),
            f("km", R.string.u_kilometre, "km", "1000"),
            f("in", R.string.u_inch, "in", "0.0254"),
            f("ft", R.string.u_foot, "ft", "0.3048"),
            f("yd", R.string.u_yard, "yd", "0.9144"),
            f("mi", R.string.u_mile, "mi", "1609.344"),
            f("nmi", R.string.u_nautical_mile, "nmi", "1852"),
        ),
        "cm", "in",
    )

    private val area = Category(
        "area", R.string.cat_area,
        listOf(
            f("cm2", R.string.u_sq_centimetre, "cm²", "0.0001"),
            f("m2", R.string.u_sq_metre, "m²", "1"),
            f("a", R.string.u_are, "a", "100"),
            f("ha", R.string.u_hectare, "ha", "10000"),
            f("km2", R.string.u_sq_kilometre, "km²", "1000000"),
            f("in2", R.string.u_sq_inch, "in²", "0.00064516"),
            f("ft2", R.string.u_sq_foot, "ft²", "0.09290304"),
            f("yd2", R.string.u_sq_yard, "yd²", "0.83612736"),
            f("ac", R.string.u_acre, "ac", "4046.8564224"),
            f("mi2", R.string.u_sq_mile, "mi²", "2589988.110336"),
        ),
        "m2", "ft2",
    )

    private val volume = Category(
        "volume", R.string.cat_volume,
        listOf(
            f("ml", R.string.u_millilitre, "mL", "0.001"),
            f("cl", R.string.u_centilitre, "cL", "0.01"),
            f("dl", R.string.u_decilitre, "dL", "0.1"),
            f("l", R.string.u_litre, "L", "1"),
            f("m3", R.string.u_cubic_metre, "m³", "1000"),
            f("tsp", R.string.u_teaspoon, "tsp", "0.00492892159375"),
            f("tbsp", R.string.u_tablespoon, "tbsp", "0.01478676478125"),
            f("floz", R.string.u_fl_oz_us, "fl oz", "0.0295735295625"),
            f("cup", R.string.u_cup_us, "cup", "0.2365882365"),
            f("pt", R.string.u_pint_us, "pt", "0.473176473"),
            f("gal", R.string.u_gallon_us, "gal", "3.785411784"),
            f("ptuk", R.string.u_pint_uk, "pt (UK)", "0.56826125"),
            f("galuk", R.string.u_gallon_uk, "gal (UK)", "4.54609"),
            f("in3", R.string.u_cubic_inch, "in³", "0.016387064"),
            f("ft3", R.string.u_cubic_foot, "ft³", "28.316846592"),
        ),
        "l", "gal",
    )

    private val mass = Category(
        "mass", R.string.cat_mass,
        listOf(
            f("mg", R.string.u_milligram, "mg", "0.000001"),
            f("g", R.string.u_gram, "g", "0.001"),
            f("kg", R.string.u_kilogram, "kg", "1"),
            f("t", R.string.u_tonne, "t", "1000"),
            f("ct", R.string.u_carat, "ct", "0.0002"),
            f("oz", R.string.u_ounce, "oz", "0.028349523125"),
            f("lb", R.string.u_pound, "lb", "0.45359237"),
            f("st", R.string.u_stone, "st", "6.35029318"),
        ),
        "kg", "lb",
    )

    private val temperature = Category(
        "temperature", R.string.cat_temperature,
        listOf(
            ConvUnit("c", R.string.u_celsius, "°C", { it }, { it }),
            ConvUnit(
                "f", R.string.u_fahrenheit, "°F",
                { it.subtract(BigDecimal(32)).multiply(BigDecimal(5)).divide(BigDecimal(9), MC) },
                { it.multiply(BigDecimal(9)).divide(BigDecimal(5), MC).add(BigDecimal(32)) },
            ),
            ConvUnit("k", R.string.u_kelvin, "K", { it.subtract(BigDecimal("273.15")) }, { it.add(BigDecimal("273.15")) }),
        ),
        "c", "f",
    )

    private val speed = Category(
        "speed", R.string.cat_speed,
        listOf(
            f("ms", R.string.u_metre_per_s, "m/s", "1"),
            ConvUnit("kmh", R.string.u_km_per_h, "km/h", { it.divide(BigDecimal("3.6"), MC) }, { it.multiply(BigDecimal("3.6")) }),
            f("mph", R.string.u_mile_per_h, "mph", "0.44704"),
            ConvUnit("kn", R.string.u_knot, "kn", { it.multiply(BigDecimal(1852)).divide(BigDecimal(3600), MC) }, { it.multiply(BigDecimal(3600)).divide(BigDecimal(1852), MC) }),
            f("fts", R.string.u_foot_per_s, "ft/s", "0.3048"),
        ),
        "kmh", "mph",
    )

    private val time = Category(
        "time", R.string.cat_time,
        listOf(
            f("ms", R.string.u_millisecond, "ms", "0.001"),
            f("s", R.string.u_second, "s", "1"),
            f("min", R.string.u_minute, "min", "60"),
            f("h", R.string.u_hour, "h", "3600"),
            f("d", R.string.u_day, "d", "86400"),
            f("wk", R.string.u_week, "wk", "604800"),
            f("mo", R.string.u_month, "mo", "2629746"),
            f("yr", R.string.u_year, "yr", "31556952"),
        ),
        "h", "min",
    )

    private val data = Category(
        "data", R.string.cat_data,
        listOf(
            f("bit", R.string.u_bit, "bit", "0.125"),
            f("B", R.string.u_byte, "B", "1"),
            f("kB", R.string.u_kilobyte, "kB", "1000"),
            f("MB", R.string.u_megabyte, "MB", "1000000"),
            f("GB", R.string.u_gigabyte, "GB", "1000000000"),
            f("TB", R.string.u_terabyte, "TB", "1000000000000"),
            f("KiB", R.string.u_kibibyte, "KiB", "1024"),
            f("MiB", R.string.u_mebibyte, "MiB", "1048576"),
            f("GiB", R.string.u_gibibyte, "GiB", "1073741824"),
            f("TiB", R.string.u_tebibyte, "TiB", "1099511627776"),
        ),
        "GB", "GiB",
    )

    private val pressure = Category(
        "pressure", R.string.cat_pressure,
        listOf(
            f("pa", R.string.u_pascal, "Pa", "1"),
            f("kpa", R.string.u_kilopascal, "kPa", "1000"),
            f("bar", R.string.u_bar, "bar", "100000"),
            f("psi", R.string.u_psi, "psi", "6894.757293168361"),
            f("atm", R.string.u_atmosphere, "atm", "101325"),
            f("mmhg", R.string.u_mmhg, "mmHg", "133.322387415"),
        ),
        "bar", "psi",
    )

    private val energy = Category(
        "energy", R.string.cat_energy,
        listOf(
            f("j", R.string.u_joule, "J", "1"),
            f("kj", R.string.u_kilojoule, "kJ", "1000"),
            f("cal", R.string.u_calorie, "cal", "4.184"),
            f("kcal", R.string.u_kilocalorie, "kcal", "4184"),
            f("wh", R.string.u_watt_hour, "Wh", "3600"),
            f("kwh", R.string.u_kilowatt_hour, "kWh", "3600000"),
            f("btu", R.string.u_btu, "BTU", "1055.05585262"),
        ),
        "kcal", "kj",
    )

    private val power = Category(
        "power", R.string.cat_power,
        listOf(
            f("w", R.string.u_watt, "W", "1"),
            f("kw", R.string.u_kilowatt, "kW", "1000"),
            f("ps", R.string.u_horsepower_metric, "PS", "735.49875"),
            f("hp", R.string.u_horsepower, "hp", "745.69987158227022"),
        ),
        "kw", "ps",
    )

    /** Fuel use: L/100 km is the base; the others are its inverse, so they get their own formulas. */
    private fun inverse(id: String, @StringRes name: Int, symbol: String, k: String): ConvUnit {
        val kk = BigDecimal(k)
        return ConvUnit(id, name, symbol, { kk.divide(it, MC) }, { kk.divide(it, MC) })
    }

    private val fuel = Category(
        "fuel", R.string.cat_fuel,
        listOf(
            f("l100", R.string.u_l_per_100km, "L/100 km", "1"),
            inverse("kml", R.string.u_km_per_l, "km/L", "100"),
            inverse("mpg", R.string.u_mpg_us, "mpg", "235.2145833"),
            inverse("mpguk", R.string.u_mpg_uk, "mpg (UK)", "282.4809363"),
        ),
        "l100", "kml",
    )

    private val angle = Category(
        "angle", R.string.cat_angle,
        listOf(
            f("deg", R.string.u_degree, "°", "1"),
            f("rad", R.string.u_radian, "rad", "57.29577951308232087679815481410517"),
            f("grad", R.string.u_gradian, "gon", "0.9"),
            f("turn", R.string.u_turn, "tr", "360"),
        ),
        "deg", "rad",
    )

    val all: List<Category> = listOf(length, mass, volume, temperature, area, speed, data, time, pressure, energy, power, fuel, angle)

    fun category(id: String?): Category = all.firstOrNull { it.id == id } ?: length
}
