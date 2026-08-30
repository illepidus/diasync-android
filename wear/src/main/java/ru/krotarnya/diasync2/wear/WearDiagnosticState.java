package ru.krotarnya.diasync2.wear;

record WearDiagnosticState(
        String headline,
        String reading,
        String snapshot,
        String display,
        String alerts,
        String error
) {}
