package com.tarasantoniuk.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
//@RequiredArgsConstructor
@Slf4j
//@Transactional(readOnly = true)
public class InfoService {
    public String getTestResult(String inputString) {

        String result = "";

        List<Integer> listSource =  new ArrayList<>();
        listSource.add(23);
        listSource.add(33);
        listSource.add(35);
        listSource.add(40);
        listSource.add(45);
        listSource.add(40);

//        //1. find duplicate in the list
//        result = findDuplicate(listSource);
//        return result;

//        //2. filter duplicate in the list
//        result = filterDuplicate(listSource);
//        return result;

        //3. Sort in reverse order
        result = sortSortInReverseOrder(listSource);
        return result;


    }

    private String findDuplicate(List<Integer> listSource) {
        List<Integer> duplicates = listSource.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        return duplicates.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private String filterDuplicate(List<Integer> listSource) {

        return listSource.stream()
                .filter(n -> n % 2 == 0)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

    }

    private String sortSortInReverseOrder(List<Integer> listSource) {
        return listSource.stream()
                .sorted(Collections.reverseOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }
}
