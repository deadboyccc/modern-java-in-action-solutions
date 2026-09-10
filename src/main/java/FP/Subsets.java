package FP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Subsets {

    static List<List<Integer>> subsets(List<Integer> list) {

        // Base case: the empty list has exactly one subset: [].
        if (list.isEmpty()) {
            List<List<Integer>> ans = new ArrayList<>();
            ans.add(Collections.emptyList());
            return ans;
        }

        // Take the first element and solve the smaller problem.
        Integer fst = list.getFirst();
        List<Integer> rest = list.subList(1, list.size());

        // Get all subsets that don't contain fst.
        List<List<Integer>> subAns = subsets(rest);

        // Create the subsets that do contain fst.
        List<List<Integer>> subAns2 = insertAll(fst, subAns);

        // Combine both groups.
        return concat(subAns, subAns2);
    }

    static List<List<Integer>> insertAll(
            Integer fst,
            List<List<Integer>> lists) {

        List<List<Integer>> result = new ArrayList<>();

        // Add fst to the beginning of every subset.
        for (List<Integer> list : lists) {
            List<Integer> copyList = new ArrayList<>();

            // Copy the subset before modifying it.
            copyList.add(fst);
            copyList.addAll(list);

            result.add(copyList);
        }

        return result;
    }

    static List<List<Integer>> concat(
            List<List<Integer>> a,
            List<List<Integer>> b) {

        // Make a new list so neither input list is modified.
        List<List<Integer>> result = new ArrayList<>(a);
        result.addAll(b);

        return result;
    }

    public static void main(String[] args) {
        List<Integer> input = List.of(1, 4, 9);

        List<List<Integer>> result = subsets(input);

        System.out.println(result);
    }
}
