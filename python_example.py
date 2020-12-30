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
