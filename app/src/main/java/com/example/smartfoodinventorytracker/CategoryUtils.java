package com.example.smartfoodinventorytracker;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CategoryUtils {

    private static String normalizeText(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    public static int getCategoryIcon(String productName) {
        productName = normalizeText(productName);

        class Category {
            int iconResId;
            List<String> keywords;
            Category(int iconResId, String... keywords) {
                this.iconResId = iconResId;
                this.keywords = Arrays.asList(keywords);
            }
        }

        List<Category> categories = new ArrayList<>();
        categories.add(new Category(R.drawable.ic_milk, "milk", "lait"));
        categories.add(new Category(R.drawable.ic_cheese, "cheese", "fromage", "shredded"));
        categories.add(new Category(R.drawable.ic_juice, "juice", "jus"));
        categories.add(new Category(R.drawable.ic_eggs, "egg", "oeuf", "oeufs"));
        categories.add(new Category(R.drawable.ic_water, "water", "eau"));
        categories.add(new Category(R.drawable.ic_oats, "oats", "oat"));

        // Fruits
        categories.add(new Category(R.drawable.ic_fruits, "apple", "banana", "orange", "mango", "apricot", "fruit", "fraise", "blueberries", "berries", "pommes", "bananes", "mangue", "abricot", "fruits"));

        // Meat & Fish
        categories.add(new Category(R.drawable.ic_meat, "meat", "steak", "beef", "poulet", "chicken", "saucisse", "sausage", "viande", "boeuf"));
        categories.add(new Category(R.drawable.ic_fish, "fish", "tuna", "salmon", "saumon", "poisson", "thon", "morue", "shrimps", "crabe", "cod", "crab"));

        // Vegetables
        categories.add(new Category(R.drawable.ic_vegetable, "lettuce", "carrot", "maïs", "spinach", "pomme de terre", "garlic", "vegetables", "laitue", "carotte", "epinard", "ail", "legumes", "potato"));

        // Nuts & Grains
        categories.add(new Category(R.drawable.ic_nuts, "peanut", "almond", "nuts", "noix", "amande", "arachide", "datte", "date"));

        // Cereals
        categories.add(new Category(R.drawable.ic_cereal, "cereal", "granola", "muesli", "cereale"));

        // Snacks
        categories.add(new Category(R.drawable.ic_snacks, "chips", "crackers", "snack", "bar", "barre", "gateau", "cookie"));

        // Canned
        categories.add(new Category(R.drawable.ic_canned, "can", "canned", "soup", "beans", "conserve", "haricots"));

        // Condiments
        categories.add(new Category(R.drawable.ic_condiments, "ketchup", "mustard", "mayo", "sauce", "vinaigrette"));

        // Bread & bakery
        categories.add(new Category(R.drawable.ic_bread, "bread", "bun", "buns", "bagel", "croissant", "brioche", "toast", "roll"));

        // Pasta
        categories.add(new Category(R.drawable.ic_pasta, "spaghetti", "penne", "macaroni", "farfalle", "rigatoni", "pasta", "pate"));

        // Dessert / Sweet
        categories.add(new Category(R.drawable.ic_dessert, "chocolate", "chocolat", "vanilla", "vanille", "cacao", "cocoa", "sweet", "sucre", "dessert"));

        // Score matching
        int bestScore = 0;
        int bestIcon = R.drawable.ic_food_default;

        for (Category category : categories) {
            int score = 0;
            for (String keyword : category.keywords) {
                if (productName.equals(keyword)) {
                    score += 3; // ✅ Strong match
                } else if (productName.contains(keyword)) {
                    // ✅ Boost "juice"/"jus" matching
                    if (keyword.equals("juice") || keyword.equals("jus")) {
                        score += 3;
                    } else {
                        score += 1;
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestIcon = category.iconResId;
            }
        }

        return bestIcon;
    }
}
