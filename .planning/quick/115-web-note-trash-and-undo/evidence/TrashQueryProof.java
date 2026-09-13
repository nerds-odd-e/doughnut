import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.hibernate.cfg.Configuration;
import java.util.*;

public class TrashQueryProof {
  static final String MEMBERSHIP = "coalesce(folder_id in (select tf.id from scratch_trashed_folder tf), false)";
  @Entity(name="ProofNote") @Table(name="scratch_trash_note")
  public static class ProofNote {
    @Id public Integer id;
    @Column(name="folder_id") public Integer folderId;
    @Column(name="deleted_at") public java.sql.Timestamp deletedAt;
    @Formula(MEMBERSHIP) public boolean trashed;
    public ProofNote() {}
  }
  public static void main(String[] args) throws Exception {
    String schema="doughnut_trash_plan_probe_"+ProcessHandle.current().pid();
    try(var admin=java.sql.DriverManager.getConnection("jdbc:mysql://127.0.0.1:3309/", "root", ""); var setup=admin.createStatement()) {
    setup.execute("create database "+schema);
    try {
    var configuration = new Configuration().addAnnotatedClass(ProofNote.class);
    configuration.setProperty("hibernate.connection.url", "jdbc:mysql://127.0.0.1:3309/"+schema);
    configuration.setProperty("hibernate.connection.username", "root");
    configuration.setProperty("hibernate.connection.password", "");
    configuration.setProperty("hibernate.hbm2ddl.auto", "none");
    configuration.setProperty("hibernate.show_sql", "true");
    try(var factory=configuration.buildSessionFactory(); var session=factory.openSession()) {
      var tx=session.beginTransaction();
      session.doWork(c -> {try(var st=c.createStatement()) {
        st.execute("create table scratch_trash_folder(id int primary key,parent_id int null,name varchar(64))");
        st.execute("create table scratch_trash_note(id int primary key,folder_id int null,deleted_at timestamp null)");
        st.execute("create view scratch_trashed_folder as with recursive trash_folders as (select f.id from scratch_trash_folder f where f.parent_id is null and lower(f.name) = '_trash' union all select child.id from scratch_trash_folder child join trash_folders parent on child.parent_id = parent.id) select id from trash_folders");
        st.execute("insert into scratch_trash_folder values (1,null,'_TrAsH'),(2,1,'Biology'),(3,null,'Projects'),(4,3,'_trash'),(5,null,'Trash'),(6,null,'_trash'),(7,6,'Other')");
        st.execute("insert into scratch_trash_note values (10,null,null),(11,1,null),(12,2,null),(13,4,null),(14,5,null),(15,7,null),(16,3,'2026-01-01'),(17,2,null)");
      }});
      var ids=session.createSelectionQuery("select n.id from ProofNote n where n.deletedAt is null and n.trashed = false order by n.id",Integer.class).getResultList();
      if(!ids.equals(List.of(10,13,14))) throw new AssertionError(ids);
      var limited=session.createSelectionQuery("select n.id from ProofNote n where n.deletedAt is null and n.trashed = false order by n.id",Integer.class).setFirstResult(1).setMaxResults(1).getSingleResult();
      if(limited != 13) throw new AssertionError(limited);
      var count=session.createSelectionQuery("select count(n) from ProofNote n where n.deletedAt is null and n.trashed = false",Long.class).getSingleResult();
      if(count != 3L) throw new AssertionError(count);
      var note=session.find(ProofNote.class,12);
      if(!note.trashed) throw new AssertionError("direct access should load trash");
      note.folderId=3;
      session.flush(); session.refresh(note);
      if(note.trashed) throw new AssertionError("move-out should refresh membership");
      System.out.println("PASS: Hibernate formula, root/case/nested distinction, legacy exclusion, direct access, pagination/count, move refresh");
      var nativeIds=session.createNativeQuery("select n.id from scratch_trash_note n where n.deleted_at is null and not coalesce(n.folder_id in (select tf.id from scratch_trashed_folder tf),false) order by n.id",Integer.class).getResultList();
      if(!nativeIds.equals(List.of(10,12,13,14))) throw new AssertionError(nativeIds);
      if(!session.find(ProofNote.class,17).trashed) throw new AssertionError("descendant starts in trash");
      session.doWork(c -> {try(var st=c.createStatement()) {st.execute("update scratch_trash_folder set parent_id=3 where id=2");}});
      session.clear();
      if(session.find(ProofNote.class,11).trashed != true) throw new AssertionError("root trash remains");
      if(session.find(ProofNote.class,17).trashed) throw new AssertionError("subtree move must recompute membership");
      session.doWork(c -> {try(var st=c.createStatement()) {st.execute("update scratch_trash_folder set name=concat('Archive',id) where parent_id is null and lower(name)='_trash'");}});
      session.clear();
      var noTrashCount=session.createSelectionQuery("select count(n) from ProofNote n where n.deletedAt is null and n.trashed = false",Long.class).getSingleResult();
      if(noTrashCount!=7L) throw new AssertionError(noTrashCount);
      System.out.println("PASS: native SQL agrees; subtree move, root rename, and zero roots recompute without stored membership");
      tx.rollback();
    }
    } finally { setup.execute("drop database "+schema); System.out.println("CLEANUP: removed isolated schema "+schema); }
    }
  }
}
