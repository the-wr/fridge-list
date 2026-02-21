package com.fridgelist.app.data.model

/**
 * The full catalog of 500 grocery icons, grouped by category.
 * Icon names are snake_case and match the SVG asset filenames.
 */
object IconCatalog {

    val all: List<IconItem> by lazy {
        buildList {
            addAll(vegetables)
            addAll(fruit)
            addAll(dairyEggs)
            addAll(meat)
            addAll(fishSeafood)
            addAll(bakery)
            addAll(dryGoods)
            addAll(oilsCondiments)
            addAll(spicesHerbs)
            addAll(drinks)
            addAll(frozen)
            addAll(snacks)
            addAll(household)
            addAll(personalCare)
            addAll(babyPet)
        }
    }

    val byCategory: Map<IconCategory, List<IconItem>> by lazy {
        all.groupBy { it.category }
    }

    private fun item(displayName: String, category: IconCategory): IconItem {
        val name = displayName.lowercase()
            .replace(" ", "_")
            .replace("&", "and")
            .replace("'", "")
            .replace("-", "_")
            .replace("/", "_")
        return IconItem(name = name, displayName = displayName, category = category)
    }

    val vegetables = listOf(
        "Tomato", "Carrot", "Onion", "Garlic", "Potato", "Sweet Potato", "Broccoli",
        "Cauliflower", "Cabbage", "Lettuce", "Spinach", "Cucumber", "Bell Pepper",
        "Zucchini", "Eggplant", "Corn", "Peas", "Green Beans", "Mushroom", "Celery",
        "Leek", "Asparagus", "Pumpkin", "Beetroot", "Radish", "Avocado", "Chili Pepper",
        "Ginger", "Artichoke", "Kale", "Spring Onion", "Brussels Sprouts", "Bok Choy",
        "Fennel", "Parsnip", "Butternut Squash", "Swiss Chard", "Edamame", "Snow Peas",
        "Watercress", "Turnip", "Okra", "Shallot", "Celeriac", "Kohlrabi", "Radicchio",
        "Endive", "Baby Spinach", "Red Cabbage", "Savoy Cabbage", "Broccolini", "Romanesco",
        "Daikon", "Bitter Melon", "Water Chestnut", "Bamboo Shoots", "Lotus Root", "Arugula",
        "Microgreens", "Taro", "Yam", "Spaghetti Squash", "Purple Onion"
    ).map { item(it, IconCategory.VEGETABLES) }

    val fruit = listOf(
        "Apple", "Banana", "Orange", "Strawberry", "Grapes", "Watermelon", "Pineapple",
        "Mango", "Peach", "Pear", "Cherry", "Blueberry", "Raspberry", "Kiwi", "Melon",
        "Plum", "Lemon", "Lime", "Grapefruit", "Coconut", "Papaya", "Fig", "Pomegranate",
        "Passion Fruit", "Apricot", "Clementine", "Cranberries", "Dates", "Dragon Fruit",
        "Blackberries", "Lychee", "Guava", "Persimmon", "Nectarine", "Tangerine", "Quince",
        "Starfruit", "Plantain", "Jackfruit", "Kumquat", "Gooseberry", "Mulberry", "Tamarind"
    ).map { item(it, IconCategory.FRUIT) }

    val dairyEggs = listOf(
        "Milk", "Butter", "Cheese", "Yogurt", "Eggs", "Heavy Cream", "Cream Cheese",
        "Sour Cream", "Mozzarella", "Parmesan", "Feta", "Ricotta", "Kefir",
        "Condensed Milk", "Whipping Cream", "Gouda", "Brie", "Cheddar", "Goat Cheese",
        "Cottage Cheese", "Buttermilk", "Halloumi", "Mascarpone", "Quark"
    ).map { item(it, IconCategory.DAIRY_EGGS) }

    val meat = listOf(
        "Chicken Breast", "Whole Chicken", "Ground Beef", "Steak", "Pork Chop", "Bacon",
        "Ham", "Sausage", "Turkey", "Lamb", "Chicken Thighs", "Chicken Wings", "Chorizo",
        "Salami", "Pepperoni", "Duck", "Veal", "Hot Dogs", "Prosciutto", "Pastrami",
        "Ground Pork", "Pork Ribs", "Ground Turkey", "Mortadella", "Pancetta"
    ).map { item(it, IconCategory.MEAT) }

    val fishSeafood = listOf(
        "Salmon", "Canned Tuna", "Shrimp", "Fish Fillet", "Sardines", "Cod", "Mussels",
        "Crab", "Mackerel", "Smoked Salmon", "Oysters", "Squid", "Trout", "Anchovies",
        "Sea Bass", "Scallops", "Octopus", "Herring", "Lobster", "Clams", "Sea Bream"
    ).map { item(it, IconCategory.FISH_SEAFOOD) }

    val bakery = listOf(
        "Bread", "Baguette", "Rolls", "Croissant", "Tortilla", "Pita", "Bagel",
        "Crackers", "Naan", "Sourdough", "Rye Bread", "English Muffin", "Muffin",
        "Brioche", "Focaccia", "Ciabatta", "Soft Pretzel", "Donut", "Waffles",
        "Flatbread", "Chapati", "Cornbread"
    ).map { item(it, IconCategory.BAKERY) }

    val dryGoods = listOf(
        "Rice", "Pasta", "Flour", "Sugar", "Salt", "Oats", "Breakfast Cereal", "Lentils",
        "Chickpeas", "Black Beans", "Canned Tomatoes", "Canned Corn", "Breadcrumbs",
        "Baking Powder", "Dark Chocolate", "Cocoa Powder", "Quinoa", "Couscous",
        "Coconut Milk", "Noodles", "Tomato Paste", "Vegetable Stock", "Yeast", "Chia Seeds",
        "Sunflower Seeds", "Brown Rice", "Whole Wheat Flour", "Cornmeal", "Red Kidney Beans",
        "White Beans", "Granola", "Raisins", "Rice Noodles", "Soba Noodles", "Pumpkin Seeds",
        "Baking Soda", "Cornstarch", "Gelatin", "Miso Paste", "Protein Powder", "Barley",
        "Buckwheat", "Polenta", "Semolina", "Dried Apricots", "Prunes", "Coconut Flakes",
        "Sesame Seeds"
    ).map { item(it, IconCategory.DRY_GOODS) }

    val oilsCondiments = listOf(
        "Olive Oil", "Vegetable Oil", "Vinegar", "Soy Sauce", "Tomato Sauce", "Ketchup",
        "Mayonnaise", "Mustard", "Hot Sauce", "Pesto", "Hummus", "Honey", "Jam",
        "Peanut Butter", "Chocolate Spread", "Tahini", "Coconut Oil", "Fish Sauce",
        "BBQ Sauce", "Teriyaki Sauce", "Maple Syrup", "Worcestershire Sauce", "Sesame Oil",
        "Balsamic Vinegar", "Apple Cider Vinegar", "Oyster Sauce", "Hoisin Sauce", "Sriracha",
        "Ranch Dressing", "Caesar Dressing", "Agave Syrup", "Rice Vinegar", "Mirin",
        "Harissa", "Curry Paste", "Gochujang", "Salsa"
    ).map { item(it, IconCategory.OILS_CONDIMENTS) }

    val spicesHerbs = listOf(
        "Black Pepper", "Cumin", "Paprika", "Oregano", "Basil", "Cinnamon", "Turmeric",
        "Chili Flakes", "Bay Leaves", "Thyme", "Rosemary", "Vanilla", "Cardamom", "Nutmeg",
        "Curry Powder", "Coriander", "Garlic Powder", "Mixed Herbs", "Saffron", "Star Anise",
        "Cloves", "Smoked Paprika", "Onion Powder", "Garam Masala", "Fennel Seeds",
        "Dried Mint", "Dried Dill", "Dried Sage", "Za'atar", "Sumac", "Allspice",
        "Mustard Seeds"
    ).map { item(it, IconCategory.SPICES_HERBS) }

    val drinks = listOf(
        "Water", "Sparkling Water", "Orange Juice", "Coffee", "Tea", "Beer", "Wine",
        "Cola", "Energy Drink", "Coconut Water", "Oat Milk", "Almond Milk", "Apple Juice",
        "Lemonade", "Kombucha", "Hot Chocolate", "Green Tea", "Herbal Tea", "Tomato Juice",
        "Sports Drink", "Prosecco", "Soy Milk", "Rice Milk", "Matcha", "Chai",
        "Ginger Beer", "Tonic Water", "Bone Broth"
    ).map { item(it, IconCategory.DRINKS) }

    val frozen = listOf(
        "Frozen Pizza", "Ice Cream", "Frozen Vegetables", "Frozen Fish", "Frozen Fries",
        "Frozen Berries", "Frozen Chicken", "Frozen Dumplings", "Frozen Waffles", "Ice",
        "Frozen Peas", "Frozen Shrimp", "Frozen Edamame", "Frozen Meatballs", "Popsicles",
        "Frozen Corn", "Frozen Spinach", "Frozen Soup", "Frozen Pot Pie"
    ).map { item(it, IconCategory.FROZEN) }

    val snacks = listOf(
        "Crisps", "Popcorn", "Mixed Nuts", "Chocolate Bar", "Cookies", "Candy",
        "Granola Bar", "Pretzels", "Rice Cakes", "Protein Bar", "Beef Jerky", "Peanuts",
        "Cashews", "Almonds", "Dried Mango", "Trail Mix", "Walnuts", "Pistachios",
        "Dark Chocolate Bar", "White Chocolate Bar", "Fruit Leather", "Sesame Snaps",
        "Pork Rinds", "Veggie Chips", "Seaweed Snacks", "Cheese Puffs", "Macadamia Nuts",
        "Dried Cranberries", "Coconut Chips"
    ).map { item(it, IconCategory.SNACKS) }

    val household = listOf(
        "Toilet Paper", "Paper Towels", "Dish Soap", "Laundry Detergent", "Trash Bags",
        "Sponge", "Aluminum Foil", "Cling Film", "Zip-lock Bags", "Cleaning Spray",
        "Dishwasher Tablets", "Fabric Softener", "Candles", "Batteries", "Light Bulb",
        "Bleach", "Glass Cleaner", "Air Freshener", "Rubber Gloves", "Matches",
        "Laundry Pods", "Stain Remover", "Shoe Polish", "Clothespins", "Toilet Brush",
        "Floor Cleaner", "Oven Cleaner", "Napkins", "Paper Plates", "Coffee Filters",
        "Mop", "Broom", "Drain Cleaner", "Toilet Cleaner", "Insect Spray",
        "Parchment Paper", "Tape", "Lint Roller", "Laundry Mesh Bag", "Steel Wool"
    ).map { item(it, IconCategory.HOUSEHOLD) }

    val personalCare = listOf(
        "Shampoo", "Conditioner", "Body Wash", "Toothpaste", "Toothbrush", "Deodorant",
        "Razor", "Tissues", "Cotton Pads", "Sunscreen", "Hand Cream", "Lip Balm",
        "Toilet Soap", "Face Wash", "Moisturizer", "Mouthwash", "Dental Floss",
        "Nail Polish Remover", "Band-Aids", "Vitamins", "Wet Wipes", "Perfume",
        "Eye Drops", "Pain Relief", "Contact Lens Solution", "Feminine Hygiene Products",
        "Hair Dye", "Q-Tips", "Hair Mask", "Face Mask", "Toner", "Nail Clippers",
        "Foot Cream", "Dry Shampoo"
    ).map { item(it, IconCategory.PERSONAL_CARE) }

    val babyPet = listOf(
        "Baby Formula", "Diapers", "Baby Food", "Baby Wipes", "Baby Lotion", "Baby Bottle",
        "Baby Shampoo", "Diaper Cream", "Pacifier", "Baby Powder", "Baby Cereal",
        "Baby Snacks", "Cat Food", "Dog Food", "Cat Treats", "Dog Treats", "Cat Litter",
        "Dog Poop Bags", "Fish Food", "Pet Shampoo", "Bird Food", "Hamster Food",
        "Rabbit Food", "Guinea Pig Food", "Turtle Food", "Ferret Food", "Cat Milk",
        "Dog Toy", "Cat Toy", "Dog Collar", "Dog Leash", "Flea Treatment", "Pet Vitamins",
        "Cat Bowl", "Dog Bowl"
    ).map { item(it, IconCategory.BABY_PET) }

    fun findByName(query: String): List<IconItem> {
        if (query.isBlank()) return all
        val lower = query.lowercase()
        return all.filter { it.displayName.lowercase().contains(lower) }
    }
}
