import copy
from datetime import date
import json
import os
import unittest
from unittest.mock import Mock, patch
import load_rent as loader


def snapshot():
    return {'source':'apt','region':'11200','month':'202608','total':2,
            'fetched_at':'2026-09-19T00:00:00+00:00','items':[
                {'sggCd':'11200','dealYear':'2026','dealMonth':'8','dealDay':'1','deposit':'12,000',
                 'monthlyRent':'0','excluUseAr':'59.99','aptNm':'테스트아파트','floor':'-1'},
                {'sggCd':'11200','dealYear':'2026','dealMonth':'8','dealDay':'2','deposit':'1,000',
                 'monthlyRent':'60','excluUseAr':'20.5','aptNm':'테스트아파트'}]}


class ParsingTest(unittest.TestCase):
    def test_month_range_crosses_year_and_rejects_invalid(self):
        self.assertEqual(loader.months_between('202512','202602'),['202512','202601','202602'])
        for start,end in [('202613','202701'),('202609','202501'),('20261','202609')]:
            with self.assertRaises(ValueError):loader.months_between(start,end)

    def test_normalization_distinguishes_jeonse_and_rent(self):
        rows=loader.normalize(snapshot())
        self.assertEqual(rows[0][4:6],('APT','JEONSE'))
        self.assertEqual(rows[0][13:15],(12000,0))
        self.assertEqual(rows[1][4:6],('APT','MONTHLY_RENT'))
        self.assertEqual(rows[1][13:15],(1000,60))
        self.assertEqual(rows[0][12],date(2026,8,1))

    def test_disclosed_identical_records_remain_distinct(self):
        data=snapshot();data['items'][1]=copy.deepcopy(data['items'][0])
        rows=loader.normalize(data)
        self.assertEqual(rows[0][2],rows[1][2])
        self.assertEqual([row[3] for row in rows],[1,2])

    def test_no_false_jeonse_from_missing_rent(self):
        data=snapshot();del data['items'][0]['monthlyRent']
        with self.assertRaises(ValueError):loader.normalize(data)

    def test_rejects_wrong_region_and_month(self):
        for key,value in [('sggCd','11440'),('dealMonth','7')]:
            data=snapshot();data['items'][0][key]=value
            with self.assertRaises(ValueError):loader.normalize(data)

    def test_pagination_accepts_server_page_size(self):
        with patch.object(loader,'fetch_page',side_effect=[(3,[{'i':'1'},{'i':'2'}]),(3,[{'i':'3'}])]),patch.object(loader.time,'sleep'):
            data=loader.download(None,'apt','unused','11200','202608')
        self.assertEqual(len(data['items']),3)

    def test_incomplete_and_changing_pages_fail(self):
        for responses in [[(2,[{'i':'1'}]),(2,[])],[(2,[{'i':'1'}]),(3,[{'i':'2'}])],[(2,[{'i':'1'}]),(2,[{'i':'1'}])]]:
            with patch.object(loader,'fetch_page',side_effect=responses),patch.object(loader.time,'sleep'):
                with self.assertRaises(loader.ImportFailure):loader.download(None,'apt','unused','11200','202608')

    def test_api_error_does_not_expose_request_secret(self):
        session=Mock();session.get.return_value.status_code=200
        session.get.return_value.content=b'<response><header><resultCode>30</resultCode><resultMsg>secret-value</resultMsg></header></response>'
        with self.assertRaises(loader.ImportFailure) as error:loader.fetch_page(session,'apt','secret-value','11200','202608',1)
        self.assertNotIn('secret-value',str(error.exception))


@unittest.skipUnless(os.environ.get('RENT_TEST_PORT'),'Requires disposable MySQL (RENT_TEST_PORT)')
class DatabaseTest(unittest.TestCase):
    def setUp(self):
        self.conn=loader.pymysql.connect(host='127.0.0.1',port=int(os.environ['RENT_TEST_PORT']),user='root',
                                         database='rent_import_test',charset='utf8mb4')
        loader.ensure_schema(self.conn)
        with self.conn.cursor() as cur:
            for _,table,_,_ in loader.SOURCES.values():cur.execute('DELETE FROM '+table)
            cur.execute('DELETE FROM rental_import_log')
        self.conn.commit()

    def tearDown(self):self.conn.close()

    def scalar(self,query):
        with self.conn.cursor() as cur:cur.execute(query);return cur.fetchone()[0]

    def test_repeat_snapshot_preserves_duplicates_without_growth(self):
        data=snapshot();data['items'][1]=copy.deepcopy(data['items'][0])
        self.assertEqual(loader.import_snapshot(self.conn,data),(0,2))
        self.assertEqual(loader.import_snapshot(self.conn,data),(2,2))
        self.assertEqual(self.scalar('SELECT COUNT(*) FROM rental_trade'),2)
        self.assertEqual(self.scalar('SELECT MAX(occurrence_no) FROM raw_apt_rent'),2)

    def test_invalid_replacement_does_not_erase_previous_month(self):
        data=snapshot();loader.import_snapshot(self.conn,data)
        data['items'][0]['deposit']='invalid'
        with self.assertRaises(Exception):loader.import_snapshot(self.conn,data)
        self.assertEqual(self.scalar('SELECT COUNT(*) FROM raw_apt_rent'),2)

    def test_database_failure_rolls_back_snapshot_delete(self):
        data=snapshot();loader.import_snapshot(self.conn,data)
        data['items'][0]['aptNm']='x'*300
        with self.assertRaises(loader.pymysql.MySQLError):loader.import_snapshot(self.conn,data)
        self.assertEqual(self.scalar('SELECT COUNT(*) FROM raw_apt_rent'),2)

    def test_three_sources_share_view_but_not_raw_tables(self):
        for source in loader.SOURCES:
            data=snapshot();data['source']=source
            loader.import_snapshot(self.conn,data)
        self.assertEqual(self.scalar('SELECT COUNT(*) FROM rental_trade'),6)
        self.assertEqual(self.scalar('SELECT COUNT(DISTINCT building_type) FROM rental_trade'),3)
        self.assertEqual(self.scalar('SELECT COUNT(*) FROM rental_import_log'),3)

    def test_empty_complete_snapshot_replaces_only_its_scope(self):
        data=snapshot();loader.import_snapshot(self.conn,data)
        other=snapshot();other['region']='11440'
        for item in other['items']:item['sggCd']='11440'
        loader.import_snapshot(self.conn,other)
        data['items']=[];data['total']=0
        loader.import_snapshot(self.conn,data)
        self.assertEqual(self.scalar('SELECT COUNT(*) FROM raw_apt_rent'),2)


if __name__=='__main__':unittest.main()
