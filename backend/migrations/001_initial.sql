-- Migration 001: Initial eFood Platform Schema + Seed Categories
\i ../schema.sql

INSERT INTO food_categories (slug, name, name_bn, icon_key, sort_order, is_active) VALUES
  ('burger', 'Burger', 'বার্গার', 'lunch_dining', 1, TRUE),
  ('pizza', 'Pizza', 'পিজ্জা', 'local_pizza', 2, TRUE),
  ('fried_chicken', 'Fried Chicken', 'ফ্রাইড চিকেন', 'set_meal', 3, TRUE),
  ('chinese', 'Chinese', 'চাইনিজ', 'ramen_dining', 4, TRUE),
  ('thai', 'Thai', 'থাই', 'soup_kitchen', 5, TRUE),
  ('indian', 'Indian', 'ইন্ডিয়ান', 'restaurant', 6, TRUE),
  ('bangladeshi', 'Bangladeshi', 'বাংলাদেশি', 'rice_bowl', 7, TRUE),
  ('biryani', 'Biryani', 'বিরিয়ানি', 'kebab_dining', 8, TRUE),
  ('fast_food', 'Fast Food', 'ফাস্ট ফুড', 'fastfood', 9, TRUE),
  ('desserts', 'Desserts', 'ডেজার্ট', 'icecream', 10, TRUE),
  ('drinks', 'Drinks', 'পানীয়', 'local_cafe', 11, TRUE),
  ('snacks', 'Snacks', 'স্ন্যাকস', 'bakery_dining', 12, TRUE)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO coupons (code, title, discount_type, discount_value, min_order_bdt, max_discount_bdt, is_active) VALUES
  ('EFOOD50', 'Flat ৳50 off orders above ৳300', 'FLAT', 50.00, 300.00, 50.00, TRUE),
  ('DHAKA20', '20% off (max ৳150) on orders ৳500+', 'PERCENT', 20.00, 500.00, 150.00, TRUE),
  ('BIRYANI15', '15% off Biryani & Bangla platters ৳400+', 'PERCENT', 15.00, 400.00, 120.00, TRUE)
ON CONFLICT (code) DO NOTHING;
