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