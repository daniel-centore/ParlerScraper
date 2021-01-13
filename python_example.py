import sqlite3
import pandas as pd
import sys

# NOTE: This might fail if you try running it while the scraper is running
# Additional documentation: https://github.com/daniel-centore/ParlerScraper

# Initialize a connection to the scraper database
con = sqlite3.connect("parler_scraper_database.db")

# Read each database table into a dataframe
users_df = pd.read_sql_query("SELECT * from users", con)
posts_df = pd.read_sql_query("SELECT * from posts", con)
links_df = pd.read_sql_query("SELECT * from links", con)
hashtags_df = pd.read_sql_query("SELECT * from hashtags", con)
scraped_ranges_df = pd.read_sql_query("SELECT * from scraped_ranges", con)

# A few examples of more advanced queries
posts_dec_2020_df = pd.read_sql_query("SELECT * from posts WHERE created_at >= '20201201000000' AND created_at < '20210101000000'", con)
users_non_private = pd.read_sql_query("SELECT * from users WHERE private_account != 1", con)
users_verified = pd.read_sql_query("SELECT * from users WHERE verified = 1", con)
posts_per_date = pd.read_sql_query("SELECT substr(created_at, 0, 9), COUNT(1) FROM posts GROUP BY 1 ORDER BY 1 DESC", con)
post_body_and_author_username = pd.read_sql_query("SELECT posts.body, users.username FROM posts JOIN users ON posts.creator_id=users.id WHERE posts.body != ''", con)
interactions_with_names = pd.read_sql_query("""SELECT
    username,
    badges,
    interactions & 1 > 0 AS i0_accepts_tips,
    interactions & 2 > 0 AS i1_static_feeds,
    interactions & 4 > 0 AS i2_dark_parley,
    interactions & 8 > 0 AS i3_influence_disabled,
    interactions & 16 > 0 AS i4_influence_admin_disabled,
    interactions & 32 > 0 AS i5_iaa_permitted,
    interactions & 64 > 0 AS i6_iaa_banning,
    interactions & 128 > 0 AS i7_self_sensitive,
    interactions & 256 > 0 AS i8_no_sensitive,
    interactions & 512 > 0 AS i9_sensitive_lockout,
    interactions & 1024 > 0 AS i10_admin_sensitive,
    interactions & 2048 > 0 AS i11_admin_discover,
    interactions & 4096 > 0 AS i12_unknown,
    interactions & 8192 > 0 AS i13_unknown
FROM users;
""", con)

# Close the connection when complete
con.close()

# Adjust printing settings
pd.set_option('display.width', 150)
pd.set_option('display.max_colwidth', 10)
pd.set_option('display.max_columns', 20)

# Print a few rows from each table
print("Users")
print(users_df.head())

print("\n\n\nPosts")
print(posts_df.head())

print("\n\n\nLinks")
print(links_df.head())

print("\n\n\nHashtags")
print(hashtags_df.head())

print("\n\n\nScraped Ranges")
print(scraped_ranges_df.head())

print("\n\n\nPosts from December 2020")
print(posts_dec_2020_df.head())

print("\n\n\nNon-private users")
print(users_non_private.head())

print("\n\n\nVerified users")
print(users_verified.head())

print("\n\n\nNumber of posts from each date")
print(posts_per_date.head())

print("\n\n\nUser interactions")
print(interactions_with_names.head())

pd.set_option('display.max_colwidth', 90)
print("\n\n\nPost Body and the Author's Username")
print(post_body_and_author_username.head())