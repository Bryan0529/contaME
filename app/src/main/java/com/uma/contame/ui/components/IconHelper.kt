package com.uma.contame.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Mapeador auxiliar que convierte nombres de iconos en formato de cadena a vectores de [ImageVector].
 */
object IconHelper {
    /**
     * Mapea una cadena de texto representando un icono a su respectivo [ImageVector] de Material Icons.
     *
     * @param iconName Nombre identificador del icono.
     * @return Instancia de [ImageVector] de Material Icons.
     */
    fun getIconByName(iconName: String): ImageVector {
        return when (iconName) {
            "restaurant" -> Icons.Default.Restaurant
            "directions_car" -> Icons.Default.DirectionsCar
            "home" -> Icons.Default.Home
            "bolt" -> Icons.Default.Bolt
            "sports_esports" -> Icons.Default.SportsEsports
            "favorite" -> Icons.Default.Favorite
            "school" -> Icons.Default.School
            "shopping_cart" -> Icons.Default.ShoppingCart
            "payments" -> Icons.Default.Payments
            "storefront" -> Icons.Default.Storefront
            "trending_up" -> Icons.Default.TrendingUp
            "work" -> Icons.Default.Work
            "card_giftcard" -> Icons.Default.CardGiftcard
            "attach_money" -> Icons.Default.AttachMoney
            "savings" -> Icons.Default.Savings
            "flight" -> Icons.Default.Flight
            "laptop" -> Icons.Default.Laptop
            "security" -> Icons.Default.Security
            "flag" -> Icons.Default.Flag
            else -> Icons.Default.Category
        }
    }
}
