package com.uma.contame

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.uma.contame.data.local.SavingsGoalEntity
import com.uma.contame.data.local.TransactionEntity
import com.uma.contame.model.SavingsGoal
import com.uma.contame.model.TransactionItem
import com.uma.contame.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("contaME", appName)
  }

  @Test
  fun `savings goal progress calculation is accurate`() {
    val goal = SavingsGoal(
      title = "Fondo",
      targetAmount = 1000.0,
      currentAmount = 500.0
    )
    assertEquals(0.5f, goal.progress, 0.001f)
    assertEquals(false, goal.isCompleted)
  }

  @Test
  fun `multi-account user scoping works on entities`() {
    val txUserA = TransactionItem(
      title = "Compra Usuario A",
      amount = 45.0,
      type = TransactionType.EXPENSE,
      categoryId = "exp_food",
      categoryName = "Alimentación",
      categoryIcon = "restaurant",
      categoryColor = "#EF4444"
    )

    val entityA = TransactionEntity.fromDomain(txUserA, userId = "user_A_123")
    assertEquals("user_A_123", entityA.userId)
    assertEquals(45.0, entityA.amount, 0.001)

    val txUserB = TransactionItem(
      title = "Venta Usuario B",
      amount = 120.0,
      type = TransactionType.INCOME,
      categoryId = "inc_sales",
      categoryName = "Ventas",
      categoryIcon = "payments",
      categoryColor = "#10B981"
    )

    val entityB = TransactionEntity.fromDomain(txUserB, userId = "user_B_456")
    assertEquals("user_B_456", entityB.userId)
    assertEquals("user_B_456", entityB.userId)
  }
}
