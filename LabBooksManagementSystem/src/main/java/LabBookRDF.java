import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import io.CSVReader;

public class LabBookRDF {
	public static void main(String[] args) throws Exception {
		//		String isbnListFilePathString = "isbn_list.csv";
		String isbnListFilePathString = args[0];
		//		List<String> isbns = Arrays.asList("9781846288845","9783540929123", "9784000074773","9784000076852","9784000077965");

		//csvファイルから、ISBN(ここでは、すべて13桁ISBNであることを前提としている)一覧を取得
		List<String> isbns = CSVReader.readCSV(isbnListFilePathString).get(0);
		//ISBN一覧から、ベースとなる知識グラフを構築
		Model combinedModel = isbns.stream().map(isbn13 -> LabBookRDF.createBookGraph(isbn13))
				.reduce(ModelFactory.createDefaultModel(), (m1, m2) -> m1.add(m2));

		//構築した知識グラフから、本のIRIをすべて取得

		//取得した本のISBNをクエリで取得
		Model resultModel = LabBookRDF.complementBookInfoFromNdl(combinedModel);

		try {
			FileOutputStream out = new FileOutputStream(args[1]);
			resultModel.write(out, "TURTLE");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public LabBookRDF() {

	}

	public static Model createBookGraph(String isbn13) {
		Model model = ModelFactory.createDefaultModel();
		//nameSpace
		final String DCTERMS = "http://purl.org/dc/terms/";
		model.setNsPrefix("dcterms", DCTERMS);
		final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
		model.setNsPrefix("rdfs", RDFS);

		//dataType
		RDFDatatype isbnDatatype = new BaseDatatype("http://ndl.go.jp/dcndl/terms/ISBN");

		// property
		Property dctermsIdentifier = model.createProperty(DCTERMS, "identifier");
		Property rdfsSubClassOf = model.createProperty(RDFS, "subClassOf");

		// resource
		Resource targetBookResource = model.createResource("http://hozo.jp/books/entity/" + isbn13 + "/1");
		Resource labBookClass = model.createResource("http://hozo.jp/books/class/book");

		// createTriple
		targetBookResource.addProperty(dctermsIdentifier, model.createTypedLiteral(isbn13, isbnDatatype));
		targetBookResource.addProperty(rdfsSubClassOf, labBookClass);

		return model;
	}

	public static Model complementBookInfoFromNdl(Model oriModel) {
		// 元のモデルの情報を補完する
		//対処となる本をすべて取得する
		// クエリ文字列
		String queryString = """
				SELECT DISTINCT ?book WHERE {
					?book <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://hozo.jp/books/class/book> .
				}
				""";
		// クエリオブジェクトの作成
		Query query = QueryFactory.create(queryString);

		// クエリの実行
		List<String> bookIriList = new ArrayList<>();
		try (QueryExecution qexec = QueryExecutionFactory.create(query, oriModel)) {
			ResultSet results = qexec.execSelect();
			bookIriList = StreamSupport.stream(
					Spliterators.spliteratorUnknownSize(results, Spliterator.ORDERED),
					false)
					.map(solution -> solution.getResource("book").getURI())
					.collect(Collectors.toList());
		}

		bookIriList.forEach(System.out::println);

		Model complementaryModel = bookIriList.stream().map(bookIri -> {
			String isbn13 = null;
			// クエリ文字列
			String queryOfIri2IsbnStr = """
					SELECT DISTINCT (STR(?isbn) AS ?isbnValue) WHERE {
						<%s> <http://purl.org/dc/terms/identifier> ?isbn .
						FILTER(datatype(?isbn) = <http://ndl.go.jp/dcndl/terms/ISBN>)
					}LIMIT 1
					""".formatted(bookIri);

			// クエリオブジェクトの作成
			Query queryOfIri2Isbn = QueryFactory.create(queryOfIri2IsbnStr);

			// クエリの実行
			try (QueryExecution qexec = QueryExecutionFactory.create(queryOfIri2Isbn, oriModel)) {
				ResultSet results = qexec.execSelect();
				QuerySolution solution = results.nextSolution();
				isbn13 = solution.get("isbnValue").toString();
			}
			// isbnが見つかれば、ndlSearchに対して問い合わせを行う
			if (Objects.nonNull(isbn13)) {
				String ndlSearchQuery = "isbn=%22"
						+ isbn13
						+ "%22%20AND%20mediatype%3d%22booklet%22%20AND%20dpid%20any%20iss-ndl-opac%20";
				NdlSearchRequest nsr = new NdlSearchRequest();
				String rdfXmlString = null;
				try {
					// ndlSearchに問い合わせる
					String ndlSearchRes = nsr.fetchNdlSearch(ndlSearchQuery);
					// 問い合わせ結果から、rdf部分を抜き出す
					rdfXmlString = nsr.extractRdfRDF(ndlSearchRes);
				} catch (Exception e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
				// 短時間での大量リクエストを避けるため、スリーブさせる
				try {
					Thread.sleep(2000); // 1000ミリ秒（1秒）スリープ
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// rdf部分が存在する場合、rdf文字列をrdfとして読み込む
				if (Objects.nonNull(rdfXmlString)) {
					Model model = nsr.loadRDFFromString(rdfXmlString);

					// SPARQL問い合わせを実行
					LabBookRDF lbrdf = new LabBookRDF();
					Model resultModel = lbrdf.constractBookInfoGraphFromNdl(model, bookIri);
					return resultModel;

				}
			}
			System.out.println(isbn13);
			return ModelFactory.createDefaultModel();

		}).reduce(ModelFactory.createDefaultModel(), (m1, m2) -> m1.add(m2));

		final Model resultModel = oriModel.add(complementaryModel);

		return resultModel;
	}

	public Model constractBookInfoGraphFromNdl(Model searchTargetModel, String bookIri) {
		String titleQueryStr = """
						CONSTRUCT {
							<%s> <http://purl.org/dc/elements/1.1/title> [
				 				<http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?title ;
				 				<http://ndl.go.jp/dcndl/terms/transcription> ?titleRuby
				 			] .
						} WHERE {
							?root <http://purl.org/dc/elements/1.1/title> ?blank .
							OPTIONAL { ?blank <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?title .}
							OPTIONAL { ?blank <http://ndl.go.jp/dcndl/terms/transcription> ?titleRuby .}
						}
				""".formatted(bookIri);
		String publisherQueryStr = """
				   		CONSTRUCT {
				   			<%s> <http://purl.org/dc/terms/publisher> [
								<http://xmlns.com/foaf/0.1/name> ?publisher;
								<http://ndl.go.jp/dcndl/terms/transcription> ?publisherRuby
							] .
				   		} WHERE {
					  		?root <http://purl.org/dc/terms/publisher> ?blank .
					  		OPTIONAL { ?blank <http://xmlns.com/foaf/0.1/name> ?publisher .}
							OPTIONAL { ?blank <http://ndl.go.jp/dcndl/terms/transcription> ?publisherRuby .}
				   		}
				""".formatted(bookIri);
		String creatorQueryStr = """
				CONSTRUCT {
					<%s> <http://purl.org/dc/terms/creator> ?creatorIRI ;
					     <http://purl.org/dc/elements/1.1/creator> ?creators .
				    ?creatorIRI <http://xmlns.com/foaf/0.1/name> ?creatorName ;
									<http://ndl.go.jp/dcndl/terms/transcription>  ?creatorRuby .
				} WHERE {
					{
						?root <http://purl.org/dc/terms/creator> ?creatorIRI.
						OPTIONAL { ?creatorIRI <http://xmlns.com/foaf/0.1/name> ?creatorName .}
					   	OPTIONAL { ?creatorIRI <http://ndl.go.jp/dcndl/terms/transcription> ?creatorRuby .}
					}UNION{
						?root <http://purl.org/dc/elements/1.1/creator> ?creators .
					}
				}
				""".formatted(bookIri);
		String subjectQueryStr = """
				CONSTRUCT {
					<%s> <http://purl.org/dc/terms/subject> ?ndlsh .
				    ?ndlsh <http://xmlns.com/foaf/0.1/name> ?subject ;
									<http://ndl.go.jp/dcndl/terms/transcription>  ?subjectRuby .
				} WHERE {
					?root <http://purl.org/dc/terms/subject> ?subjectIRI.
					BIND(IF(STRSTARTS(STR(?subjectIRI), "http://id.ndl.go.jp/auth/ndlsh/"), ?subjectIRI, "") AS ?ndlsh)
					FILTER(?ndlsh != "")
					OPTIONAL { ?ndlsh <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?subject .}
				   	OPTIONAL { ?ndlsh <http://ndl.go.jp/dcndl/terms/transcription> ?subjectRuby .}


				}
				""".formatted(bookIri);
		String ndcQueryStr = """
				CONSTRUCT {
					<%s> <http://www.wikidata.org/prop/direct/P31> ?NDC9 .
				} WHERE {
					?root <http://purl.org/dc/terms/subject> ?subjectIRI.

					BIND(IF(STRSTARTS(STR(?subjectIRI), "http://id.ndl.go.jp/class/ndc"), ?subjectIRI, "") AS ?NDC9)
					FILTER(?NDC9 != "")

				}
				""".formatted(bookIri);
		// 3. クエリ結果を使って新しいグラフの作成
		Model resultModel = Arrays
				.asList(titleQueryStr, publisherQueryStr, creatorQueryStr, subjectQueryStr, ndcQueryStr)
				.stream().map(q -> QueryFactory.create(q))
				.map(q -> QueryExecutionFactory.create(q, searchTargetModel).execConstruct())
				.reduce(ModelFactory.createDefaultModel(), (m1, m2) -> m1.add(m2));

		return resultModel;

	}
}
