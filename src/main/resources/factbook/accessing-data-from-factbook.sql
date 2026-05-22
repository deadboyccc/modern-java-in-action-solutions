-- 1
-- DataGrip PostgreSQL Query
--
-- Uses named parameters: :start_date
-- When executing, DataGrip will prompt you to enter:
-- Example value: 2017-02-01

select date,

       -- to_char() formats numeric values into readable text.
       -- 99G999G999G999 pattern:
       --   G = group separator (comma in most locales)
       --   9 = digit placeholder
       -- Example: 1234567 -> 1,234,567
       to_char(shares, '99G999G999G999')   as shares,

       -- Format trades with thousand separators
       -- 99G999G999 = up to 9 digits with commas
       to_char(trades, '99G999G999')       as trades,

       -- Format dollar amounts with currency symbol
       -- L = local currency symbol (e.g., $)
       -- Example: $1,234,567
       to_char(dollars, 'L99G999G999G999') as dollars

from factbook

where
  -- Filter for the specified month
  -- DATE LITERAL must be in quotes: '2017-02-01'
  -- PostgreSQL will automatically interpret YYYY-MM-DD format
    date >= '2017-02-01'::date

  -- Upper bound: less than one month later
  -- Creates a clean month-only range: 2017-02-01 <= date < 2017-03-01
  and date < '2017-02-01'::date + interval '1 month'

order by date asc;

-- DataGrip-compatible version of the query.
--
-- This query generates a full calendar for a month,
-- then LEFT JOINs it with the factbook table.
--
-- Result:
-- Every day of the month appears in the output,
-- even if no row exists in the factbook table.
--
-- DataGrip parameter:
-- :start
--
-- Example value to enter when prompted:
-- 2017-02-01


-- 2
-- SELECT chooses which columns/data we want to retrieve.
select

    -- calendar.entry comes from generate_series().
    --
    -- CAST converts the timestamp value into a DATE type.
    cast(calendar.entry as date) as date,

    -- coalesce() replaces NULL with a fallback value.
    --
    -- If shares is NULL, return 0 instead.
    coalesce(shares, 0)          as shares,

    -- Same logic for trades.
    coalesce(trades, 0)          as trades,

    -- Format dollar values nicely.
    --
    -- First:
    -- coalesce(dollars, 0)
    -- replaces NULL with 0.
    --
    -- Then:
    -- to_char()
    -- formats the number using:
    --
    -- L = local currency symbol
    -- G = group separator
    --
    -- Example:
    -- $1,234,567
    to_char(
            coalesce(dollars, 0),
            'L99G999G999G999'
    )                            as dollars


-- FROM clause.
from (

         /*
          * Generate the target month's calendar.
          *
          * generate_series(start, stop, step)
          *
          * Creates one row per day.
          *
          * Example output:
          * 2017-02-01
          * 2017-02-02
          * 2017-02-03
          * ...
          */

         select generate_series(

                    -- Starting date.
                        to_date(':start', 'YYYY-MM-DD'),
                    -- Ending date.
                    --
                    -- Add 1 month,
                    -- then subtract 1 day
                    -- so we stop at the last day
                    -- of the target month.
                        to_date(':start', 'YYYY-MM-DD')
                            + interval '1 month'
                            - interval '1 day',
                    -- Step interval:
                    -- generate one row every 1 day.
                        interval '1 day'
                ) as entry) as calendar


         -- LEFT JOIN keeps ALL calendar dates,
-- even when no matching row exists in factbook.
         left join factbook
                   on factbook.date = calendar.entry


-- Sort results chronologically.
order by date;

-- 2.1
-- DataGrip-compatible version using a CTE.
--
-- CTE = Common Table Expression.
--
-- WITH lets us create a temporary named result set
-- that can be referenced later in the query.
--
-- This improves readability and structure.


-- Create a temporary result set called "calendar".
with calendar as (
    /*
     * generate_series(start, stop, step)
     *
     * Generates one row per day.
     *
     * Example:
     * 2017-02-01
     * 2017-02-02
     * 2017-02-03
     * ...
     */

    select generate_series(

               -- Start date entered from DataGrip parameter.
                   to_date(':start', 'YYYY-MM-DD'),
               -- End date:
               -- start + 1 month - 1 day
               --
               -- This gives the final day of the month.
                   to_date(':start', 'YYYY-MM-DD')
                       + interval '1 month'
                       - interval '1 day',

               -- Generate rows every 1 day.
                   interval '1 day'
           ) as entry)
select *
from calendar;


-- Main query.
select

    -- Convert generated timestamp into DATE type.
    cast(calendar.entry as date) as date,

    -- Replace NULL shares with 0.
    coalesce(shares, 0)          as shares,

    -- Replace NULL trades with 0.
    coalesce(trades, 0)          as trades,

    -- Replace NULL dollars with 0,
    -- then format nicely as currency.
    to_char(
            coalesce(dollars, 0),
            'L99G999G999G999'
    )                            as dollars


-- Read from the CTE.
from calendar


         -- LEFT JOIN ensures every generated calendar day appears,
-- even when no matching factbook row exists.
         left join factbook
                   on factbook.date = calendar.entry


-- Sort chronologically.
order by date;