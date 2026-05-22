-- cleanup
drop table if exists factbook;

-- Start a transaction block so that if any step fails,
-- the database rolls back to its original state.

-- Create an ad-hoc staging table.
-- Notice that columns with commas and dollar signs are defined as TEXT
-- because standard numeric types cannot parse formatting characters directly.
create table factbook
(
    year    int,
    date    date,
    shares  text,
    trades  text,
    dollars text
);

--- do the import from the wizard here

-- Alter the data types of the columns to their proper numeric representations.
begin;
alter table factbook
    -- 1. Clean and convert the 'shares' column
    alter shares
        type bigint
        using replace(shares, ',', '')::bigint,

    -- 2. Clean and convert the 'trades' column
    alter trades
        type bigint
        using replace(trades, ',', '')::bigint,

    -- 3. Clean and convert the 'dollars' column
    alter dollars
        type numeric
        -- substring(..., from 2) strips the leading '$' sign starting from character 2
        using substring(replace(dollars, ',', '') from 2)::numeric;

-- Commit the transaction to save all changes permanently.
commit;

-- accessing data
select *
from factbook;