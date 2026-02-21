package com.fridgelist.app.data.model

/**
 * The default 8x11 grid layout populated on first-run when the user chooses
 * "Default Grid". Positions are 0-indexed (row, col).
 */
object DefaultGrid {

    val columns = 8
    val rows = 11

    data class DefaultTile(val row: Int, val col: Int, val iconName: String, val taskName: String)

    val tiles: List<DefaultTile> = listOf(
        // Row 0: Vegetables
        DefaultTile(0, 0, "tomato", "Tomato"),
        DefaultTile(0, 1, "carrot", "Carrot"),
        DefaultTile(0, 2, "onion", "Onion"),
        DefaultTile(0, 3, "garlic", "Garlic"),
        DefaultTile(0, 4, "potato", "Potato"),
        DefaultTile(0, 5, "bell_pepper", "Bell Pepper"),
        DefaultTile(0, 6, "cucumber", "Cucumber"),
        DefaultTile(0, 7, "lettuce", "Lettuce"),
        // Row 1: Vegetables (cont.)
        DefaultTile(1, 0, "broccoli", "Broccoli"),
        DefaultTile(1, 1, "spinach", "Spinach"),
        DefaultTile(1, 2, "mushroom", "Mushroom"),
        DefaultTile(1, 3, "avocado", "Avocado"),
        // Row 2: Fruit
        DefaultTile(2, 0, "apple", "Apple"),
        DefaultTile(2, 1, "banana", "Banana"),
        DefaultTile(2, 2, "orange", "Orange"),
        DefaultTile(2, 3, "strawberry", "Strawberry"),
        DefaultTile(2, 4, "grapes", "Grapes"),
        DefaultTile(2, 5, "lemon", "Lemon"),
        DefaultTile(2, 6, "pear", "Pear"),
        // Row 3: Dairy & Eggs
        DefaultTile(3, 0, "milk", "Milk"),
        DefaultTile(3, 1, "eggs", "Eggs"),
        DefaultTile(3, 2, "butter", "Butter"),
        DefaultTile(3, 3, "cheese", "Cheese"),
        DefaultTile(3, 4, "yogurt", "Yogurt"),
        DefaultTile(3, 5, "cream_cheese", "Cream Cheese"),
        // Row 4: Meat
        DefaultTile(4, 0, "chicken_breast", "Chicken Breast"),
        DefaultTile(4, 1, "ground_beef", "Ground Beef"),
        DefaultTile(4, 2, "bacon", "Bacon"),
        DefaultTile(4, 3, "sausage", "Sausage"),
        DefaultTile(4, 4, "ham", "Ham"),
        // Row 5: Fish & Seafood
        DefaultTile(5, 0, "salmon", "Salmon"),
        DefaultTile(5, 1, "canned_tuna", "Canned Tuna"),
        DefaultTile(5, 2, "shrimp", "Shrimp"),
        DefaultTile(5, 3, "cod", "Cod"),
        // Row 6: Bakery
        DefaultTile(6, 0, "bread", "Bread"),
        DefaultTile(6, 1, "rolls", "Rolls"),
        DefaultTile(6, 2, "crackers", "Crackers"),
        DefaultTile(6, 3, "tortilla", "Tortilla"),
        // Row 7: Dry Goods
        DefaultTile(7, 0, "rice", "Rice"),
        DefaultTile(7, 1, "pasta", "Pasta"),
        DefaultTile(7, 2, "flour", "Flour"),
        DefaultTile(7, 3, "sugar", "Sugar"),
        DefaultTile(7, 4, "salt", "Salt"),
        DefaultTile(7, 5, "oats", "Oats"),
        DefaultTile(7, 6, "canned_tomatoes", "Canned Tomatoes"),
        DefaultTile(7, 7, "lentils", "Lentils"),
        // Row 8: Drinks
        DefaultTile(8, 0, "water", "Water"),
        DefaultTile(8, 1, "coffee", "Coffee"),
        DefaultTile(8, 2, "tea", "Tea"),
        DefaultTile(8, 3, "orange_juice", "Orange Juice"),
        DefaultTile(8, 4, "beer", "Beer"),
        // Row 9: Household
        DefaultTile(9, 0, "toilet_paper", "Toilet Paper"),
        DefaultTile(9, 1, "dish_soap", "Dish Soap"),
        DefaultTile(9, 2, "trash_bags", "Trash Bags"),
        DefaultTile(9, 3, "laundry_detergent", "Laundry Detergent"),
        DefaultTile(9, 4, "paper_towels", "Paper Towels"),
        // Row 10: Personal Care
        DefaultTile(10, 0, "shampoo", "Shampoo"),
        DefaultTile(10, 1, "conditioner", "Conditioner"),
        DefaultTile(10, 2, "toothpaste", "Toothpaste"),
        DefaultTile(10, 3, "deodorant", "Deodorant"),
    )
}
