import org.repodriller.RepoDriller;
import org.repodriller.RepositoryMining;
import org.repodriller.Study;
import org.repodriller.filter.range.Commits;
import org.repodriller.scm.GitRepository;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;

public class RuStudy implements Study {
/***
    private static String earliestCommit = "3d7cd77e442ce34eaac8a176ae8be17669498ebc"; // dec 10 2015
    private static String commitAtRelease = "e8a0123241f0d397d39cd18fcc4e5e7edde22730"; // dec 22 2016
    private static String commitYearAfterRelease = "766bd11c8a3c019ca53febdcd77b2215379dd67d"; // jan 4 2018
***/

    private static String earliestCommit = "f3d6973f41a7d1fb83029c9c0ceaf0f5d4fd7208"; // aug 27 2017
    private static String commitAtRelease = "3b72af97e42989b2fe104d8edbaee123cdf7c58f"; // oct 12 2017
    private static String commitYearAfterRelease = "328886ba2eaf0d3ac7e78ef3ba27eb296d0af3c0"; // nov 20 2017

/***
    private static Calendar noBugsIntroducedBefore =  new GregorianCalendar(2016,11,22);
    private static Calendar noBugsIntroducedAfter = new GregorianCalendar(2017,5,22);
***/

    private static Calendar noBugsIntroducedBefore =  new GregorianCalendar(2017,9,12);
    private static Calendar noBugsIntroducedAfter = new GregorianCalendar(2017,10,0);

    public static void main(String[] args) {
        new RepoDriller().start(new RuStudy());
    }


    public void execute() {
        ConcurrentHashMap<String ,Integer> defectsMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> churnMap  = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> sizeMap  = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> ownerMap = new ConcurrentHashMap<>();

        BugVisitor bugVisitor = new BugVisitor(noBugsIntroducedBefore, noBugsIntroducedAfter, defectsMap);
        ChurnVisitor churnVisitor = new ChurnVisitor(churnMap);
        SizeVisitor sizeVisitor = new SizeVisitor(sizeMap);
        OwnerVisitor ownerVisitor = new OwnerVisitor(ownerMap);

        // churn, owner, size visitor
        new RepositoryMining()
                .in(GitRepository.singleProject("/home/michael/Documents/athens/rust-repo/rust"))
                .through(Commits.range(earliestCommit, commitAtRelease))
                .process(ownerVisitor).process(churnVisitor).process(sizeVisitor)
                .mine();

        // bug visitor
        new RepositoryMining()
                .in(GitRepository.singleProject("/home/michael/Documents/athens/rust-repo/rust"))
                .through(Commits.range(commitAtRelease, commitYearAfterRelease))
                .process(bugVisitor)
                .mine();

        OwnershipCalculator oc = new OwnershipCalculator(ownerMap);

        ConcurrentHashMap<String, Integer> minorMap = oc.getMinorMap();
        ConcurrentHashMap<String, Integer> majorMap = oc.getMajorMap();
        ConcurrentHashMap<String, Double> ownershipMap = oc.getOwnershipMap();

        try {
            TSVCreator.write("results.tsv", churnMap, sizeMap, defectsMap, minorMap, majorMap, ownershipMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
