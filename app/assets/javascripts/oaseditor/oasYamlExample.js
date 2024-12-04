export const exampleOasYaml = `
openapi: 3.0.3
info:
  title: NPS Interface Specification to validate a P800 Reference.
  description: |-
    # Usage Terms
    These interfaces are business-critical interfaces for HMRC and DWP, supporting thousands of staff - all consumption, or change in consumption levels, should be registered and fully approved (see Registered Consumers below).
  contact:
    name: HMRC NPS Live Service
    url: http://{placeholderforurl}.hmrc.gov.uk
    email: user@hmrc.gov.uk
  version: 0.0.2
  license:
    name: HMRC
    url: https://license.example.hmrc.gov.uk
servers:
  - url: https://{hostname}:{port}
    description: >-
      The actual environment values to use will differ depending on the environment.
    variables:
      hostname:
        default: hostname
      port:
        enum:
          - '7008'
        default: '7008'
tags: []
paths:
  /nps-json-service/nps/v1/api/reconciliation/p800/{identifier}/{paymentNumber}:
    get:
      summary: NPS Interface Specification to validate a P800 Reference and retrieve Payment Reference data.
      description: |-
        # Purpose
        This API provides the capability to validate a P800 Reference when provided and if valid, returns the relative Payment Reference details. This endpoint requires Mutual Authentication over TLS 1.2.
        - Example URL to NPS:  https://{hostname}:{port}/nps-json-service/nps/v1/api/reconciliation/p800/{identifier}/{paymentNumber} <br>

        # Volumes & Registered Consumers
        This API is consumed by the following 'Registered Consumers' who would all need to be impacted when a new consumer with an associated new load first registers to use the service, or an uplift is required to the API. Each 'Registered Consumer' below will receive an additional Security Spec. document that outlines how to connect to the various environments as well as any consumer-specific authorisation/authentication details - this is unique to their connection.

         | Consumer | Average API Calls Per Hour | Peak API Calls Per Hour | Peak TPS |
         |----------|------------------------|---------------------|----------|
         | DIGITAL | xxx | xxx | xxx |

         *TPS = Transactions per second

         ## Version Log
         | Version | Date | Author | Description |
         |---------|------|--------|-------------|
         | 0.0.0 | 15/12/2023 | Modernising Repayments Team | Initial Draft |
         | 0.0.1 | 18/12/2023 | Modernising Repayments Team | Updated paymentNumber to be required in path. |
         | 0.0.2 | 21/12/2023 | Modernising Repayments Team | URL Refinement & Refactor. |
      operationId: P800ReferenceCheck
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
        - $ref: '#/components/parameters/GovUkOriginatorId'
        - $ref: '#/components/parameters/Identifier'
        - $ref: '#/components/parameters/PaymentNumber'
      responses:
        '200':
          description: Successful Response
          headers:
            CorrelationId:
              $ref: '#/components/headers/CorrelationId'
          content:
            application/json;charset=UTF-8:
              schema:
                $ref: '#/components/schemas/paymentReferenceResponse'
        '400':
          headers:
            correlationId:
              schema:
                $ref: '#/components/schemas/correlationId'
              description: A unique ID used for traceability purposes
              required: true
          description: Bad Request
          content:
            application/json;charset=UTF-8:
              schema:
                $ref: '#/components/schemas/errorResponse_400'
              examples:
                Actual_Response:
                  value:
                    failures:
                      - reason: HTTP message not readable
                        code: '400.2'
                      - reason: Constraint Violation - Invalid/Missing input parameter
                        code: '400.1'
        '403':
          headers:
            correlationId:
              schema:
                $ref: '#/components/schemas/correlationId'
              description: A unique ID used for traceability purposes
              required: true
          description: 'Forbidden '
          content:
            application/json;charset=UTF-8:
              schema:
                $ref: '#/components/schemas/errorResponse_403'
              examples:
                Actual_Response:
                  value:
                    reason: Forbidden
                    code: '403.2'
        '404':
          $ref: '#/components/responses/errorResponseNotFound'
        '422':
          headers:
            CorrelationId:
              schema:
                $ref: '#/components/schemas/correlationId'
          description: Unprocessable Entity
          content:
            application/json;charset=UTF-8:
              schema:
                $ref: '#/components/schemas/errorResponse'
              examples:
                Actual_Response:
                  value:
                    failures:
                      - reason: Payment has already been claimed.
                        code: '422'
        '500':
          $ref: '#/components/responses/errorResponseBadGateway'
components:
  securitySchemes:
    basicAuth:
      type: http
      scheme: basic
      description: HTTPS with MTLS1.2
  headers:
    CorrelationId:
      required: true
      schema:
        $ref: '#/components/schemas/correlationId'
  parameters:
    CorrelationId:
      description:
        Correlation ID - used for traceability purposes - note that this
        value in the response matches that received in the request to allow correlation.
      in: header
      name: CorrelationId
      required: true
      schema:
        $ref: '#/components/schemas/correlationId'
    GovUkOriginatorId:
      description: Identity of the Originating System that made the API call.
      in: header
      name: gov-uk-originator-id
      required: true
      schema:
        $ref: '#/components/schemas/govUkOriginatorId'
    Identifier:
      name: identifier
      in: path
      required: true
      description: The identifier of which could be either a Temporary Reference
        Number (TRN) or a National Insurance Number (NINO).
      schema:
        '$ref': '#/components/schemas/identifierParameter'
    PaymentNumber:
      in: path
      name: paymentNumber
      required: true
      schema:
        '$ref': '#/components/schemas/paymentReferenceResponse/properties/paymentNumber'
  responses:
    errorResponseBadGateway:
      headers:
        CorrelationId:
          schema:
            $ref: '#/components/schemas/correlationId'
          description: A unique ID used for traceability purposes
          required: true
      description: Internal Server Error
    errorResponseNotFound:
      headers:
        correlationId:
          schema:
            $ref: '#/components/schemas/correlationId'
          description: A unique ID used for traceability purposes
          required: true
      description: The requested resource could not be found
  schemas:
    correlationId:
      description:
        Correlation ID - used for traceability purposes - note that this
        value in the response matches that received in the request to allow correlation.
      example: e470d658-99f7-4292-a4a1-ed12c72f1337
      format: uuid
      type: string
    govUkOriginatorId:
      description: Identity of the Originating System that made the API call
      type: string
      enum:
        - da2_tbc_digital
      example: da2_tbc_digital
    errorResponse:
      description: Error Response Payload for this API
      title: Error Response
      type: object
      properties:
        failures:
          '$ref': '#/components/schemas/errorResponseFailure'
    errorResponseFailure:
      description: Array of Error Response Failure Object in Error Response.
      title: Failure Object in Error Response
      type: array
      items:
        '$ref': '#/components/schemas/errorResourceObj'
    errorResourceObj:
      type: object
      required:
        - code
        - reason
      properties:
        reason:
          minLength: 1
          description: Displays the reason of the failure passed from NPS.
          type: string
          maxLength: 120
        code:
          minLength: 1
          description:
            The error code representing the error that has occurred passed
            from NPS.
          type: string
          maxLength: 10
    identifierParameter:
      description: The identifier of which could be either a Temporary Reference
        Number (TRN) e.g. 00A00000 or a National Insurance Number (NINO) e.g. AA000001A.
      type: string
      pattern: '^(((?:[ACEHJLMOPRSWXY][A-CEGHJ-NPR-TW-Z]|B[A-CEHJ-NPR-TW-Z]|G[ACEGHJ-NPR-TW-Z]|[KT][A-CEGHJ-MPR-TW-Z]|N[A-CEGHJL-NPR-SW-Z]|Z[A-CEGHJ-NPR-TW-Y])[0-9]{6}[A-D]?)|([0-9]{2}[A-Z]{1}[0-9]{5}))$'
      example: AA000001A
    nationalInsuranceNumber:
      title: nationalInsuranceNumber
      type: string
      minLength: 8
      maxLength: 9
      pattern: '^((?:[ACEHJLMOPRSWXY][A-CEGHJ-NPR-TW-Z]|B[A-CEHJ-NPR-TW-Z]|G[ACEGHJ-NPR-TW-Z]|[KT][A-CEGHJ-MPR-TW-Z]|N[A-CEGHJL-NPR-SW-Z]|Z[A-CEGHJ-NPR-TW-Y])[0-9]{6}[A-D]?)$'
      example: AA000001A
    temporaryReferenceNumber:
      title: temporaryReferenceNumber
      description:
        Temporary Reference Number (TRN) - unique for an individual and
        used where the individual does not hold a National Insurance Number (NINO)
        for whatever reason.
      type: string
      minLength: 8
      maxLength: 8
      pattern: '^([0-9]{2}[A-Z]{1}[0-9]{5})$'
      example: 00A00000
    paymentReferenceResponse:
      description: Success response payload for this API.
      required:
        - reconciliationIdentifier
        - paymentNumber
        - payeNumber
        - taxDistrictNumber
        - payableAmount
        - paymentAmount
      properties:
        reconciliationIdentifier:
          type: integer
          description: The reconciliation identifier for a reconciliation calculation
            that occurred on an individual's account.
          maximum: 65534
          minimum: 1
          example: 12
        paymentNumber:
          type: integer
          description: The identifier for the payment.
          example: 789
          maximum: 2147483646
          minimum: 1
        payeNumber:
          type: string
          description: Denotes the PAYE reference number associated with a taxpayer.
          example: 123/A56789
          maxLength: 10
          minLength: 1
          pattern: ^[A-Z0-9/ ]+$
        taxDistrictNumber:
          type: string
          description:  unique reference number that identifies where an employer's or pension provider's tax records are stored.
          enum:
            - ABERDEEN 3 DISTRICT (797) (closed)
            - ABERYSTWYTH RECOVERY (426)
            - ACCRINGTON DISTRICT (003) (closed)
            - ACCRINGTON DISTRICT (233) (closed)
            - ACCRINGTON RECOVERY (845) (CLOSED)
            - ACTON DISTRICT (004) (closed)
            - ALTRINCHAM RECOVERY (476)
            - ANDOVER DISTRICT (098) (closed)
            - ANGLIA RECOVERY (283)
            - ARDROSSAN DISTRICT (800) (closed)
            - AYR RECOVERY (802) (closed)
            - BALLYMENA DISTRICT (956) (closed)
            - BANBURY DISTRICT (044) (closed)
            - BANGOR INTEGRATED OFFICE (045) (CLOSED)
            - BANGOR RECOVERY (427) (closed)
            - BARNET DISTRICT (039) (closed)
            - BARNSLEY 1 DISTRICT (046) (closed)
            - BARNSLEY 2 DISTRICT (047) (closed)
            - BARNSLEY RECOVERY (377)
            - BARNSTAPLE 1 DISTRICT (048) (closed)
            - BARROW RECOVERY (477) (closed)
            - BASINGSTOKE DISTRICT (050) (CLOSED)
            - BATH 2 DISTRICT (716) (closed)
            - BATH INTEGRATED OFFICE (279) (closed)
            - BATH RECOVERY (420) (CLOSED)
            - BEDFORD 1 DISTRICT (053) (closed)
            - BEDFORD RECOVERY (228) (CLOSED)
            - BEDS AND WEST HERTS AREA (COMPLIANCE) (438)
            - BEDS AND WEST HERTS AREA (SERVICE) (419)
            - BELFAST 1 DISTRICT (932) (closed)
            - BELFAST 3 DISTRICT (934) (closed)
            - BELFAST 4 DISTRICT (935) (closed)
            - BELFAST 7 (EX SWANSEA 3) (877)
            - BELFAST 7 (EX WREXHAM 2) (893)
            - BELFAST 7 (TELFORD PRIORSLEE) (971)
            - BELFAST RECOVERY (952)
            - BERKSHIRE AREA (COMPLIANCE) (610)
            - BERKSHIRE AREA (LONDON SA 268601) (686)
            - BERKSHIRE AREA (SERVICE) (592)
            - BERKSHIRE RECOVERY (006)
            - BETHNAL GREEN DISTRICT (056) (closed)
            - BEXLEYHEATH DISTRICT (042) (closed)
            - BIRKENHEAD RECOVERY (466)
            - BIRMINGHAM  4 DISTRICT (458) (closed)
            - BIRMINGHAM 10 DISTRICT (464) (closed)
            - BIRMINGHAM 2 DISTRICT (451) (closed)
            - BIRMINGHAM 4 DISTRICT (020) (closed)
            - BIRMINGHAM 6 (NMW KENSINGT)DIST (460) (closed)
            - BIRMINGHAM 7 DISTRICT (078) (closed)
            - BIRMINGHAM 8 DIST(NMW ROMFORD) (462) (closed)
            - BIRMINGHAM RECOVERY (202)
            - BIRMINGHAM SOLIHULL AREA (COMPLIANCE) (450)
            - BIRMINGHAM SOLIHULL AREA (SERVICE) (068)
            - BIRMINGHAM VALUATION (221) (closed)
            - BISHOP AUCKLAND DISTRICT (082) (closed)
            - BISHOP'S STORTFORD RECOVERY (026) (CLOSED)
            - BLACKBURN 1 DISTRICT (085) (closed)
            - BLACKPOOL RECOVERY (454) (closed)
            - BLACKPOOL WYREVIEW TDO (089) (closed)
            - BOGNOR REGIS DISTRICT (014) (closed)
            - BOLTON 1 DISTRICT (092) (closed)
            - BOLTON 3 DISTRICT (094) (closed)
            - BOLTON RECOVERY (455)
            - BOOTLE 1 (EX HULL 4) DISTRICT (857)
            - BOOTLE 1(EX MIDDLESBROUGH 3) (963)
            - BOOTLE 1(SUNDERLAND 3)DISTRICT (964)
            - BOOTLE 1(X BIRKENHEAD 3)DISTRICT (858)
            - BOSTON DISTRICT (096) (closed)
            - BOSTON RECOVERY (276) (closed)
            - BOURNEMOUTH 1 DISTRICT (097) (closed)
            - BOURNEMOUTH RECOVERY (051)
            - BRADFORD 3 DISTRICT (104) (closed)
            - BRADFORD 4 DISTRICT (105) (closed)
            - BRADFORD RECOVERY (LOCAL) (352)
            - BRECON DISTRICT (112) (closed)
            - BRIDGEND DISTRICT (113) (closed)
            - BRIDGWATER DISTRICT (025) (closed)
            - BRIDLINGTON RECOVERY (015) (closed)
            - BRIGHTON & HOVE RECOVERY (001)
            - BRIGHTON 1 DISTRICT (116) (closed)
            - BRIGHTON 2 DISTRICT (117) (CLOSED)
            - BRIGHTON 4 DISTRICT (109) (closed)
            - BRISTOL 1 DISTRICT (119) (closed)
            - BRISTOL 2 DISTRICT (023) (closed)
            - BRISTOL 3 DISTRICT (033) (closed)
            - BRISTOL 9 DISTRICT (041) (closed)
            - BRISTOL AND N SOMERSET AREA (BOURNEMOUTH (049)
            - BRISTOL AND N SOMERSET AREA (COMPLIANCE) (036)
            - BRISTOL AND N SOMERSET AREA (W S MARE) (034)
            - BRISTOL LBO (CT) (660)
            - BRISTOL LBO DISTRICT (035) (closed)
            - BRISTOL LBO DISTRICT (642) (closed)
            - BRISTOL RECOVERY (153)
            - BROMLEY 1 DISTRICT (122) (closed)
            - BROMLEY RECOVERY (326) (CLOSED)
            - BUCKIE DISTRICT (810) (closed)
            - BURNLEY INTEGRATED OFFICE (123) (closed)
            - BURNLEY RECOVERY (401) (CLOSED)
            - BURTON-ON-TRENT DISTRICT (124) (closed)
            - BURY DISTRICT (121) (closed)
            - BURY ST EDMUNDS DISTRICT (129) (closed)
            - CAMBRIDGE RECOVERY (251)
            - CAMBRIDGESHIRE AREA (COMPLIANCE) (570)
            - CAMBRIDGESHIRE AREA (SERVICE) (126)
            - CAMBRIDGESHIRE RECOVERY (261)
            - CAMDEN TOWN DISTRICT (134) (closed)
            - CANNOCK DISTRICT (135) (closed)
            - CANTERBURY RECOVERY (127)
            - CARDIFF 1 TSO (099) (closed)
            - CARDIFF 3 DISTRICT (216) (closed)
            - CARDIFF 4 (LNDN) DISTRICT (021) (closed)
            - CARDIFF 4 TDO (551)
            - CARDIFF 6 DISTRICT (138) (closed)
            - CARDIFF 6/ OXFORD 1 DISTRICT (545) (closed)
            - CARDIFF 6/ OXFORD 2 DISTRICT (546) (closed)
            - CARDIFF 6/ OXFORD 3 DISTRICT (547) (closed)
            - CARDIFF 7 DISTRICT (022) (closed)
            - CARLISLE DISTRICT (139) (closed)
            - CARMARTHEN DISTRICT (140) (closed)
            - CAVENDISH 1 DISTRICT (142) (closed)
            - CAVENDISH 3 DISTRICT (146) (closed)
            - CAVENDISH 3 DISTRICT (564) (closed)
            - CENTRAL BANKING SERVICES (059)
            - CENTRAL ENGLAND REGIONAL OFFICE (537)
            - CENTRAL LONDON AREA (CT Only) (623)
            - CENTRAL LONDON AREA (SERVICE) (301)
            - CENTRAL YORKSHIRE AREA (COMPLIANCE) (388)
            - CENTRAL YORKSHIRE AREA (SERVICE) (567)
            - CENTRE 1 AREA (COMPLIANCE) (976)
            - CENTRE 1 AREA (EMPLOYERS) (875)
            - CENTRE 1 AREA (SERVICE ) (961)
            - CENTRE FOR NON RESIDENTS (BOOTLE) (922)
            - CENTRE FOR NON RESIDENTS (NEWCASTLE) (978)
            - CENTRE FOR NON RESIDENTS (NOTTINGHAM) (904)
            - CENTRE FOR REVENUE INTELLIGENCE (900)
            - CHAPEL WHARF AREA (COMPLIANCE) (923)
            - CHAPEL WHARF AREA (COMPLIANCE) (924)
            - CHAPEL WHARF AREA (SERVICE) (951)
            - CHATHAM DISTRICT (147) (closed)
            - CHATHAM RECOVERY (128)
            - CHELMSFORD DISTRICT (149) (closed)
            - CHELMSFORD RECOVERY (252) (CLOSED)
            - CHELTENHAM 1 DISTRICT (151) (closed)
            - CHELTENHAM 2 DISTRICT (218) (closed)
            - CHESTER 1 DISTRICT (152)
            - CHESTERFIELD 2 DISTRICT (213) (closed)
            - CHESTERFIELD RECOVERY (403)
            - CHICHESTER DISTRICT (156) (closed)
            - CHICHESTER INTEGRATED OFF (118) (CLOSED)
            - CHICHESTER RECOVERY (027)
            - CHIPPENHAM DISTRICT (157) (CLOSED)
            - CITY  6 DISTRICT (165) (closed)
            - CITY 16 DISTRICT (676) (676) (closed)
            - CITY 16 DISTRICT(175) (closed)
            - CITY 17 DISTRICT (176) (closed)
            - CITY A LBO (CT) (497) (closed)
            - CITY A LBO DISTRICT (161) (closed)
            - CITY A LBO DISTRICT (166) (closed)
            - CITY A LBO DISTRICT (172) (closed)
            - CITY A LBO DISTRICT (173) (closed)
            - CITY B LBO DISTRICT  (367) (closed)
            - CITY B LBO DISTRICT (163) (closed)
            - CITY B LBO DISTRICT (168) (closed)
            - CITY B LBO DISTRICT (344) (closed)
            - CITY B LBO DISTRICT (350) (closed)
            - CITY B LBO DISTRICT (351) (closed)
            - CITY C LARGE BUSINESS OFFICE (178) (closed)
            - CITY C LBO DISTRICT  (179) (closed)
            - CITY C LBO DISTRICT (763) (closed)
            - CITY D LBO (CT) (277) (closed)
            - CITY D LBO DISTRICT (164) (closed)
            - CITY D LBO DISTRICT (170) (closed)
            - CITY D LBO DISTRICT(180) (closed)
            - CITY E LBO (CT) (488) (closed)
            - CITY E LBO DISTRICT (171) (closed)
            - CITY E LBO DISTRICT (177) (closed)
            - CITY E LBO DISTRICT (182) (closed)
            - CITY E LBO DISTRICT (353) (closed)
            - CITY E LBO DISTRICT (365) (closed)
            - CITY E LBO DISTRICT (393) (closed)
            - CITY F LARGE BUSINESS OFFICE (396) (closed)
            - CITY F LBO DISTRICT (314) (closed)
            - CITY OF LONDON AREA (CT Only) (680)
            - CITY OF LONDON AREA (SERVICE) (305)
            - CITY OF LONDON RECOVERY (037) (closed)
            - CIVIL RECOVERY SECTION EDINBURGH (927)
            - CLACTON DISTRICT (188) (closed)
            - CLAPHAM DISTRICT (189) (closed)
            - CLERKENWELL DISTRICT (145) (closed)
            - COATBRIDGE DISTRICT (811) (closed)
            - COLCHESTER DISTRICT (191) (closed)
            - CORNWALL AND PLYMOUTH AREA (CLAIMS) (860)
            - CORNWALL AND PLYMOUTH AREA (COMPLIANCE) (474)
            - CORNWALL AND PLYMOUTH AREA (LONDON SA) (842)
            - CORNWALL AND PLYMOUTH AREA (SERVICE) (470)
            - CORNWALL AND PLYMOUTH RECOVERY (081)
            - COUNTY COURT CENTRAL UNIT (CCCU) (114)
            - COVENT GARDEN DISTRICT (194) (closed)
            - COVENTRY 2 DISTRICT (196) (closed)
            - COVENTRY 3 DISTRICT (197) (CLOSED)
            - COVENTRY RECOVERY (264)
            - CRAIGAVON RECOVERY (955)
            - CRAWLEY DISTRICT (217) (closed)
            - CREWE 1 DISTRICT (198) (closed)
            - CREWE 2 DISTRICT (199) (closed)
            - CREWE INTEGRATED OFFICE (187)
            - CRI (841) Also see (900)
            - CROYDON 4 DISTRICT (203) (closed)
            - CROYDON 5 DISTRICT (208) (closed)
            - CUMBRIA AREA (COMPLIANCE) (356)
            - CUMBRIA AREA (SERVICE) (783)
            - CUMBRIA RECOVERY (457)
            - Centre for Non-Residents (796)
            - DAGENHAM DISTRICT (229) (CLOSED)
            - DEBT MANAGEMENT UNIT (TYNESIDE) (130)
            - DERBY 1 (NMW BIRMINGHAM)DIST (250) (closed)
            - DERBY 1 TSO (235) (closed)
            - DERBY 3 DISTRICT (253) (closed)
            - DEREHAM DISTRICT (255) (closed)
            - DEVON AREA (TORQUAY) (070)
            - DEVON RECOVERY (076)
            - DEWSBURY DISTRICT (239) (closed)
            - DONCASTER 1 DISTRICT (241) (closed)
            - DONCASTER 2 DISTRICT (242) (closed)
            - DONCASTER 3 DISTRICT (243) (closed)
            - DONCASTER INTEGRATED OFFICE (125) (closed)
            - DONCASTER RECOVERY (404)
            - DORCHESTER DISTRICT (244) (closed)
            - DORSET AND S WILTSHIRE AREA (COMPLIANCE) (578)
            - DORSET AND S WILTSHIRE AREA (SERVICE) (503)
            - DOVER DISTRICT (240) (closed)
            - DUDLEY 1 DISTRICT (246) (closed)
            - DUDLEY DISTRICT (247) (closed)
            - DUMBARTON DISTRICT (822) (closed)
            - DUMFRIES 1 DISTRICT (823) (closed)
            - DUMFRIES 2 DISTRICT (824) (closed)
            - DUMFRIES INTEGRATED OFFICE (986) (closed)
            - DUMFRIES RECOVERY (231)
            - DUNDEE 3 DIST. (806) (closed)
            - DUNDEE 3 DISTRICT (827) (closed)
            - DUNDEE RECOVERY (803)
            - DUNFERMLINE DISTRICT (829) (closed)
            - DUNOON DISTRICT (831) (closed)
            - DUNSTABLE DISTRICT (227)
            - DURHAM INTEGRATED OFFICE (249) (CLOSED)
            - E CHESHIRE AND S LANCS AREA (CT ONLY) (682)
            - E CHESHIRE AND S LANCS AREA (SERVICE) (582)
            - E HAMPSHIRE AND WIGHT AREA (COMPLIANCE) (486)
            - E HAMPSHIRE AND WIGHT AREA (SERVICE) (581)
            - EALING BROADWAY 1 TSO (296)
            - EALING RECOVERY (158)
            - EAST 1 TSO (896)
            - EAST 3 LONDON TSO (898)
            - EAST COMPLIANCE OFFICE (258) (closed)
            - EAST HERTS WEST ESSEX AREA (COMPLIANCE) (084)
            - EAST HERTS WEST ESSEX AREA (HARLOW) (321)
            - EAST KENT AREA (COMPLIANCE) (136)
            - EAST KENT AREA (SERVICE) (230)
            - EAST LANCASHIRE AREA (CT ONLY) (095)
            - EAST LANCASHIRE AREA (SERVICE) (106)
            - EAST LONDON AREA (COMPLIANCE) (733)
            - EAST LONDON AREA (SERVICE) (717)
            - EASTBOURNE DISTRICT (254) (closed)
            - EASTERN COUNTIES RECOVERY (278)
            - EDGWARE DISTRICT (271) (closed)
            - EDINBURGH 2 DIST (Edinburgh) (833) (closed)
            - EDINBURGH 4 DIST. (835) (closed)
            - EDINBURGH 7 DIST. (838) (closed)
            - EDINBURGH 8 DIST. (839) (closed)
            - EDINBURGH 8 DISTRICT (812) (closed)
            - EDINBURGH HOLYROOD DISTRICT (975)
            - EDINBURGH LBO (CT) (834)
            - EDINBURGH LBO DISTRICT (832) (closed)
            - EDINBURGH LBO DISTRICT (836) (closed)
            - EDINBURGH RECOVERY (804)
            - EDMONTON DISTRICT (270) (closed)
            - EMPLOYER DATA EXCEPTION UNIT (132)
            - ENFIELD 1 DISTRICT (259) (closed)
            - ENFIELD 2 DIST. (274) (closed)
            - ENFIELD RECOVERY (002)
            - ENFORCEMENT AND INSOLVENCY (SERVICE) (880)
            - ENFORCEMENT AND INSOLVENCY BELFAST (627)
            - ENNISKILLEN DISTRICT (960) (closed)
            - EPSOM DISTRICT (272) (CLOSED)
            - ESSEX RECOVERY (262)
            - EUSTON 1 DISTRICT (260) (closed)
            - EUSTON RECOVERY (169) (closed)
            - EUSTON SQUARE TDO (374) (closed)
            - EXETER 1 DISTRICT (263) (closed)
            - EXETER 3 DISTRICT (265) (closed)
            - EXETER 4 DISTRICT (266) (closed)
            - EXETER RECOVERY (052) (closed)
            - FALKIRK DISTRICT (851) (closed)
            - FARNHAM RECOVERY (028) (closed)
            - FICO (A&C) see Head Office under FICO. (672)
            - FOREIGN ENTERTAINERS UNIT (562)
            - FROME DISTRICT (280) (closed)
            - FURNESS INTEGRATED OFFICE (269) (closed)
            - GAINSBOROUGH DISTRICT (282) (closed)
            - GALASHIELS DISTRICT (853) (closed)
            - GATESHEAD 1 DISTRICT (511) (closed)
            - GATESHEAD 2 DISTRICT (514) (closed)
            - GATESHEAD 3 DISTRICT (515) (closed)
            - GLASGOW 2 DISTRICT (Glasgow) (855) (closed)
            - GLASGOW 3 DISTRICT (818) (closed)
            - GLASGOW 4 DISTRICT (819) (closed)
            - GLASGOW 4 DISTRICT (881) (closed)
            - GLASGOW 5 DISTRICT (882) (closed)
            - GLASGOW 6 DISTRICT (821) (closed)
            - GLASGOW 6 DISTRICT (883) (closed)
            - GLASGOW 7 DISTRICT (876) (closed)
            - GLASGOW 8 DISTRICT (884) (closed)
            - GLASGOW 9 DISTRICT (889) (closed)
            - GLASGOW LBO (CT) (854)
            - GLASGOW LBO DISTRICT (702) (closed)
            - GLASGOW RECOVERY (805)
            - GLENROTHES (EX BOLTON 3) (958)
            - GLENROTHES (EX DUNDEE 4) (866)
            - GLENROTHES (EX MANCHESTER 10) (962)
            - GLENROTHES IRO (972)
            - GLOS AND N WILTSHIRE AREA (COMPLIANCE) (066)
            - GLOS AND N WILTSHIRE AREA (SERVICE) (214)
            - GLOS AND N WILTSHIRE AREA (SERVICE) (613)
            - GLOUCESTERSHIRE AND WILTSHIRE RECOVERY (067)
            - GOOLE DISTRICT (285) (closed)
            - GRANTHAM DISTRICT (286) (closed)
            - GRANTHAM RECOVERY (328)
            - GRAVESEND DISTRICT (287) (closed)
            - GRAYS DISTRICT (288) (closed)
            - GREAT YARMOUTH 2 DISTRICT (788) (closed)
            - GREATER BELFAST AREA (CLAIMS) (974)
            - GREATER BELFAST AREA (COMPLIANCE) (933)
            - GREATER BELFAST AREA (SERVICE) (925)
            - GREENFORD DISTRICT (295) (closed)
            - GREENOCK DISTRICT (874) (closed)
            - GREENWICH DISTRICT (289) (closed)
            - GRIMSBY 2 DISTRICT (291) (closed)
            - GUILDFORD 1 DISTRICT (292) (closed)
            - GUILDFORD 2 DISTRICT (293) (closed)
            - HACKNEY DISTRICT (310) (closed)
            - HALESOWEN DISTRICT (248) (closed)
            - HALIFAX 2 DIST. (312) (closed)
            - HALIFAX 2 DISTRICT (313) (closed)
            - HAMILTON 1 DISTRICT (886) (closed)
            - HAMILTON 2 DISTRICT (890) (closed)
            - HAMILTON DISTRICT (891) (closed)
            - HAMMERSMITH 1 DISTRICT (316) (closed)
            - HAMPSTEAD DISTRICT (317) (closed)
            - HARLESDEN DISTRICT (770) (closed)
            - HARLEY DISTRICT (148) (closed)
            - HARLOW DISTRICT (299) (closed)
            - HARROGATE DISTRICT (318) (closed)
            - HARROGATE RECOVERY (381)
            - HARROW DISTRICT (319) (closed)
            - HARTLEPOOL DIST (Peterlee CC) (759) (Closed)
            - HASTINGS DISTRICT (320) (closed)
            - HATFIELD DISTRICT (308) (closed)
            - HAVANT DISTRICT (300) (closed)
            - HAVERFORDWEST INTEGRATED OFFICE (315) (closed)
            - HAVERFORDWEST RECOVERY (181)
            - HAWICK DISTRICT (885) (closed)
            - HAYES DISTRICT (306) (closed)
            - HEMEL HEMPSTEAD DISTRICT (739) (closed)
            - HENDON DISTRICT (322) (closed)
            - HEREFORD INTEGRATED OFFICE (323) (closed)
            - HEREFORD RECOVERY (232) (closed)
            - HERTFORD DISTRICT (324) (closed)
            - HERTFORDSHIRE RECOVERY (024)
            - HEXHAM INTEGRATED OFFICE (325) (closed)
            - HEXHAM RECOVERY (415) (closed)
            - HIGH WYCOMBE 2 DISTRICT (340) (closed)
            - HIGHBURY DISTRICT (108) (closed)
            - HOLBORN DISTRICT (329) (closed)
            - HOLLAND PARK DISTRICT (302) (closed)
            - HOLLOWAY DISTRICT (330) (closed)
            - HORNSEY DISTRICT (331) (closed)
            - HORSHAM DISTRICT (332) (closed)
            - HORSHAM RECOVERY (031)
            - HOUNSLOW DISTRICT (309) (closed)
            - HUDDERSFIELD 2 DISTRICT (307) (closed)
            - HUDDERSFIELD DISTRICT (335) (closed)
            - HUDDERSFIELD RECOVERY (354) (CLOSED)
            - HULL 2 DISTRICT (337) (closed)
            - HULL 3 DISTRICT (338) (closed)
            - HULL 4 DISTRICT (339) (closed)
            - HUMBER AREA (COMPLIANCE) (336)
            - HUMBER AREA (LONDON SA 229001) (290) (closed)
            - HUMBER AREA (LONDON SA 262401) (624)
            - HUMBER AREA (SERVICE) (391)
            - HUMBER RECOVERY (355)
            - HUNTINGDON DISTRICT (341) (closed)
            - HYDE DISTRICT (342) (closed)
            - ILFORD DISTRICT(343) (CLOSED)
            - INVERNESS 1(NMW MAIDSTONE) DIST (887) (closed)
            - INVERNESS 2 DISTRICT (888) (closed)
            - INVERNESS INTEGRATED OFFICE (988) (closed)
            - INVERNESS RECOVERY (826)
            - IPSWICH 1 DISTRICT (345) (closed)
            - IPSWICH 3 DISTRICT (348) (closed)
            - IR TRUSTS - LONDON (087) (CLOSED)
            - IR TRUSTS - MANCHESTER (425) (CLOSED)
            - IR TRUSTS - NOTTINGHAM (484)
            - IR TRUSTS - TRURO (712)
            - IRVINE DISTRICT (816) (closed)
            - ISLE OF WIGHT INTEGRATED OFFICE (517) (CLOSED)
            - ISLE OF WIGHT RECOVERY (079) (CLOSED)
            - ISLINGTON DISTRICT (347) (closed)
            - KENSINGTON 2 TDO (150) (closed)
            - KENSINGTON RECOVERY (038) (closed)
            - KENT RECOVERY (830)
            - KIDDERMINSTER DISTRICT (361) (closed)
            - KILMARNOCK(NMW PORTSMOUTH) DIST (894) (closed)
            - KING'S LYNN INTEGRATED OFFICE (435) (closed)
            - KINGS CROSS 1 TSO (304)
            - KINGS CROSS 2 TDO (656) (closed)
            - KINGSTON RECOVERY (101)
            - KINGSTON-U-THAMES DISTRICT (363) (closed)
            - KINGSTON-UPON-THAMES DISTRICT (298) (closed)
            - KIRKCALDY DISTRICT (895) (closed)
            - LAMBETH DISTRICT (369) (closed)
            - LANARK DISTRICT (902) (closed)
            - LANCASTER DISTRICT (385) (closed)
            - LANCASTER RECOVERY (482) (closed)
            - LAUNCESTON DISTRICT (386)
            - LBS FINANCIAL LONDON OFFICE (268)
            - LEAMINGTON DISTRICT (387) (closed)
            - LEEDS 2 DIST. (389) (closed)
            - LEEDS 2 DISTRICT (378) (closed)
            - LEEDS 3 DISTRICT (394) (closed)
            - LEEDS 4 DISTRICT (395) (closed)
            - LEEDS 5 DISTRICT (392) (closed)
            - LEEDS LBO (CT) (294)
            - LEEDS LBO DISTRICT (215) (closed)
            - LEEDS LBO DISTRICT (566) (closed)
            - LEEDS LBO DISTRICT (637) (637) (closed)
            - LEEDS RECOVERY (357)
            - LEEK DISTRICT (399) (closed)
            - LEICESTER 1 DISTRICT (400) (closed)
            - LEICESTER 4 DISTRICT (382) (closed)
            - LEICESTER 5 DISTRICT (383) (closed)
            - LEICESTER 6 DISTRICT (384) (closed)
            - LEICESTER 7 (EX BARNSLEY 2) (863)
            - LEICESTER 7 (EX NOTTINGHAM 6) (969)
            - LEICESTER 7 (EX NOTTINGHAM 7) (970)
            - LEICESTER 7 (EX YARMOUTH 2) (957)
            - LEICS AND NORTHANTS AREA (CLAIMS) (856)
            - LEICS AND NORTHANTS AREA (COMPLIANCE) (110)
            - LEICS AND NORTHANTS AREA (CROYDON SA) (864)
            - LEICS AND NORTHANTS AREA (IBTO) (892)
            - LEICS AND NORTHANTS AREA (SERVICE) (267)
            - LEYTON DISTRICT (405) (closed)
            - LINCOLN RECOVERY (257)
            - LINCOLNSHIRE AREA (COMPLIANCE) (373)
            - LINCOLNSHIRE AREA (SERVICE) (475)
            - LISBURN AREA (COMPLIANCE) (539)
            - LISBURN AREA (SERVICE) (953)
            - LISBURN RECOVERY (071)
            - LIVERPOOL  3 DISTRICT (411) (closed)
            - LIVERPOOL  7 DIST. (422) (closed)
            - LIVERPOOL  7 DISTRICT (376) (closed)
            - LIVERPOOL  8 DISTRICT (416) (closed)
            - LIVERPOOL 10 DISTRICT (418) (closed)
            - LIVERPOOL 2 DISTRICT (410) (closed)
            - LIVERPOOL LBO (005) (CT ONLY)
            - LIVERPOOL LBO DISTRICT (144) (closed)
            - LIVERPOOL LBO DISTRICT (174) (closed)
            - LIVERPOOL LBO DISTRICT (495) (closed)
            - LIVERPOOL RECOVERY (459)
            - LIVERPOOL SOUTH RECOVERY (467) (closed)
            - LLANELLI DISTRICT (429) (closed)
            - LONDON CENTRAL RECOVERY (011)
            - LONDON COMPLIANCE PROJECT UNIT (LIU) (655)
            - LONDON COMPLIANCE PROJECTS UNIT (256)
            - LONDON EAST RECOVERY (284)
            - LONDON NORTH RECOVERY (090)
            - LONDON NORTH WEST RECOVERY (167)
            - LONDON PROVINCIAL 12 DISTRICT (907) (Closed)
            - LONDON PROVINCIAL 13 DISTRICT (908) (closed)
            - LONDON PROVINCIAL 14 DISTRICT (909) (closed)
            - LONDON PROVINCIAL 15 DISTRICT (910) (closed)
            - LONDON PROVINCIAL 16 DISTRICT (911) (closed)
            - LONDON PROVINCIAL 17 DISTRICT (912) (closed)
            - LONDON PROVINCIAL 18 DISTRICT (913) (closed)
            - LONDON PROVINCIAL 20 DISTRICT (844) (closed)
            - LONDON PROVINCIAL 23 DISTRICT (847) (closed)
            - LONDON PROVINCIAL 24 DISTRICT (848) (closed)
            - LONDON PROVINCIAL 25 DISTRICT (849) (closed)
            - LONDON PROVINCIAL 3 DISTRICT (993) (closed)
            - LONDON PROVINCIAL 30 DISTRICT (850) (closed)
            - LONDON PROVINCIAL 34 TSO (225) (closed)
            - LONDON PROVINCIAL 5 DISTRICT (995) (closed)
            - LONDON PROVINCIAL 6 TSO (996) (closed)
            - LONDON PROVINCIAL 8 DISTRICT (998) (closed)
            - LONDON PROVINCIAL 9 DISTRICT (999) (closed)
            - LONDON REGIONAL OFFICE (501)
            - LONDON SOUTH EAST RECOVERY (010)
            - LONDON SOUTH RECOVERY (303)
            - LONDONDERRY RECOVERY (954)
            - LOTHIANS AREA (CLAIMS) (867)
            - LOTHIANS AREA (COMPLIANCE) (843)
            - LOTHIANS AREA (SERVICE) (846)
            - LOUTH DISTRICT (431) (closed)
            - LOWESTOFT DISTRICT (432) (closed)
            - LP  1 DISTRICT (991) (closed)
            - LP  4 DISTRICT (994) (closed)
            - LUDLOW DISTRICT (433) (closed)
            - LUTON 1 DISTRICT (434) (closed)
            - LUTON 2 (NMW CAMBRIDGE) DIST (379) (closed)
            - MACCLESFIELD INTEGRATED OFFICE (436)
            - MAIDSTONE 1 DISTRICT (439) (closed)
            - MANCHESTER  1 DIST. (440) (closed)
            - MANCHESTER  2 DISTRICT (441) (closed)
            - MANCHESTER  3 DISTRICT (442) (closed)
            - MANCHESTER  4 DISTRICT (424) (closed)
            - MANCHESTER  4 DISTRICT (443) (closed)
            - MANCHESTER 13 DISTRICT (452) (closed)
            - MANCHESTER 14 DISTRICT (453) (closed)
            - MANCHESTER 17 DISTRICT (456) (closed)
            - MANCHESTER AREA (COMPLIANCE) (421)
            - MANCHESTER AREA (EMPLOYERS) (080)
            - MANCHESTER CASTLEFIELD TSO (446) (CLOSED)
            - MANCHESTER DEANSGATE DISTRICT (408) (closed)
            - MANCHESTER DEANSGATE TSO (950) (closed)
            - MANCHESTER EUSTON(NMW BEL) DIST (273) (closed)
            - MANCHESTER IRWELL TSO (947) (closed)
            - MANCHESTER LBO (CT) (397)
            - MANCHESTER MILLBANK DISTRICT (479) (closed)
            - MANCHESTER RECOVERY (407)
            - MANCHESTER STRAND DISTRICT (689) (closed)
            - MANCHESTER TRINITY TSO (949) (closed)
            - MANCHESTER VICTORIA DISTRICT (723) (closed)
            - MANCHESTER WESTMIN(NMW ST) DIST (761) (closed)
            - MEDWAY INTEGRATED OFFICE (219) (closed)
            - MELTON MOWBRAY DISTRICT (469) (closed)
            - MERRY HILL RECOVERY (205) (closed)
            - MERSEYSIDE AREA (COMPLIANCE) (423)
            - MERSEYSIDE AREA (SERVICE) (428)
            - MERTHYR TYDFIL DISTRICT (472) (closed)
            - MERTHYR TYDFIL RECOVERY (183)
            - MIDDLESBROUGH 1 DISTRICT (471) (closed)
            - MIDDLESBROUGH 2 DISTRICT (478) (closed)
            - MIDLANDS WEST AREA (COMPLIANCE) (693)
            - MIDLANDS WEST AREA (SERVICE) (653)
            - MILTON KEYNES 1 DISTRICT (371) (closed)
            - MILTON KEYNES 1 INTEGRATED OFF (370) (CLOSED)
            - MILTON KEYNES RECOVERY (813)
            - MORPETH RECOVERY (029) (closed)
            - MOTHERWELL DISTRICT (903) (closed)
            - MUTUAL ASSISTANCE RECOVERY OF DEBT TEAM (088)
            - N IRELAND COUNTIES AREA (COMPLIANCE) (987)
            - N IRELAND COUNTIES AREA (LONDON SA) (977)
            - N IRELAND COUNTIES AREA (SERVICE) (916)
            - NEASDEN DISTRICT (500) (closed)
            - NEATH DISTRICT (516) (closed)
            - NEW MALDEN DISTRICT (502) (closed)
            - NEWARK DISTRICT (505) (CLOSED)
            - NEWBURY DISTRICT (506) (CLOSED)
            - NEWCASTLE 1(NMW MIDDLESBR) DIST (510) (closed)
            - NEWCASTLE 2 DIST. (508) (closed)
            - NEWCASTLE 2 DISTRICT (524) (closed)
            - NEWCASTLE 3 DISTRICT (509) (closed)
            - NEWCASTLE 6 DISTRICT (512) (closed)
            - NEWCASTLE LBO (CT) (583)
            - NEWCASTLE LBO (CT) (701)
            - NEWCASTLE RECOVERY (360)
            - NEWPORT 1 DISTRICT (518) (closed)
            - NEWPORT 2 DISTRICT (519) (closed)
            - NEWPORT RECOVERY (160)
            - NEWRY DISTRICT (982) (closed)
            - NEWRY RECOVERY (040) (closed)
            - NEWTON ABBOT DISTRICT (521) (closed)
            - NORFOLK AREA (COMPLIANCE) (529)
            - NORFOLK AREA (SERVICE) (531)
            - NORTH EAST 2 DISTRICT (928) (closed)
            - NORTH EAST 3 (LONDON) DISTRICT (929) (closed)
            - NORTH EAST 5 (LONDON) DISTRICT (931) (closed)
            - NORTH LONDON AREA (Corporation Tax Only) (226)
            - NORTH LONDON AREA (SERVICE) (209)
            - NORTH WALES AREA (COMPLIANCE) (793)
            - NORTH WALES AREA (SERVICE) (914)
            - NORTH WALES RECOVERY (437)
            - NORTH WEST 1 DISTRICT (498) (closed)
            - NORTH WEST 2 DISTRICT (490) (closed)
            - NORTH WEST 3 DISTRICT (491) (closed)
            - NORTH WEST 4 DISTRICT (492) (closed)
            - NORTH WEST 5 DISTRICT (493) (closed)
            - NORTH WEST 6 DISTRICT (494) (closed)
            - NORTH WEST LONDON AREA (SERVICE) (461)
            - NORTH YORKSHIRE AREA (COMPLIANCE) (791)
            - NORTH YORKSHIRE AREA (LONDON SA 211501) (115)
            - NORTH YORKSHIRE AREA (SERVICE) (585)
            - NORTHAMPTON 1 DISTRICT (525)
            - NORTHAMPTON 2 DISTRICT (523) (closed)
            - NORTHEAST METROPOLITAN AREA (FILM UNIT) (905)
            - NORTHEAST METROPOLITAN AREA (SERVICE) (120)
            - NORTHUMBRIA AREA (COMPLIANCE) (CT Only) (513)
            - NORTHUMBRIA AREA (SERVICE) (504)
            - NORTHWICH DISTRICT (526)
            - NORWICH 1 DISTRICT (527) (closed)
            - NORWICH 2 DISTRICT (528) (closed)
            - NORWICH 4 DISTRICT (372) (closed)
            - NORWOOD DISTRICT (530) (closed)
            - NOTTINGHAM 3 DISTRICT (533) (closed)
            - NOTTINGHAM 4 DIST. (536) (closed)
            - NOTTINGHAM 4 DISTRICT (534) (closed)
            - NOTTINGHAM 5 DISTRICT (535) (closed)
            - NOTTINGHAM AND DERBY RECOVERY (311)
            - NOTTINGHAM LBO (CT) (572)
            - NOTTINGHAM LBO DISTRICT (573) (closed)
            - NOTTS AND DERBYSHIRE AREA (COMPLIANCE) (532)
            - NOTTS AND DERBYSHIRE AREA (SERVICE) (507)
            - NUNEATON DISTRICT (540) (closed)
            - NW LANCASHIRE AREA (BLACKPOOL) (065)
            - NW LANCASHIRE AREA (COMPLIANCE) (449)
            - NW MIDLANDS AND SHROPS AREA ((SERVICE) (671)
            - NW MIDLANDS AND SHROPS AREA (COMPLIANCE) (778)
            - OIL TAXATION OFFICE (349)
            - OLDHAM 2 DISTRICT (542) (closed)
            - OLDHAM PENNINE TSO (522) (closed)
            - OLDHAM PRIORY TDO (541) (closed)
            - OLDHAM RECOVERY (409)
            - OSWESTRY DISTRICT (544) (CLOSED)
            - OXFORD 1 DISTRICT (184) (closed)
            - OXFORD 2 DISTRICT (185) (closed)
            - OXFORD 3 DISTRICT (186) (closed)
            - OXFORD INTEGRATED OFFICE (520) (closed)
            - OXFORD RECOVERY (210)
            - OXON AND BUCKS AREA (COMPLIANCE) (402)
            - OXON AND BUCKS AREA (MILTON KEYNES) (362)
            - PAIGNTON DISTRICT (561) (closed)
            - PAYMENT HELPLINE CUMBERNAULD (143)
            - PAYMENT HELPLINE SHIPLEY (141)
            - PD  2 DISTRICT (941) (closed)
            - PD  3 DISTRICT (942) (closed)
            - PD  4 DISTRICT (943) (closed)
            - PD  5 DISTRICT (944) (closed)
            - PD  6 DISTRICT (945) (closed)
            - PD  7 DISTRICT (946) (closed)
            - PD  8 DISTRICT (555) (closed)
            - PD  9 DISTRICT (554) (closed)
            - PD 10 DISTRICT (553) (closed)
            - PENDLE DISTRICT (565) (CLOSED)
            - PENRITH DISTRICT (568) (closed)
            - PENZANCE DISTRICT (557)
            - PERTH DISTRICT (918) (closed)
            - PETERBOROUGH 1 DISTRICT (569) (closed)
            - PETERBOROUGH LBO (CT) (549)
            - PETERBOROUGH LBO DISTRICT (558) (closed)
            - PETERHEAD DISTRICT (919) (closed)
            - PIMLICO DISTRICT/REG 42/49 UNIT (559) (closed)
            - PINNER DISTRICT (563) (closed)
            - PLYMOUTH 1 DISTRICT (473) (closed)
            - PLYMOUTH 3 DISTRICT (560) (closed)
            - PLYMOUTH RECOVERY (057)
            - PONTEFRACT DISTRICT (574) (closed)
            - PONTYPOOL DISTRICT (575) (closed)
            - PONTYPRIDD 1 DISTRICT (576) (closed)
            - PONTYPRIDD DISTRICT (552) (closed)
            - PORTHMADOG DISTRICT (580) (closed)
            - PORTSMOUTH 1 DISTRICT (485) (closed)
            - PORTSMOUTH 3 DIST/ PORTSMOUTH CC (487) (closed)
            - PORTSMOUTH MARITIME TSO (815) (closed)
            - PORTSMOUTH RECOVERY (058)
            - PRESTON 1 DISTRICT (587) (closed)
            - PRESTON 2 DIST. (588) (closed)
            - PRESTON 2 DISTRICT (155) (closed)
            - PRESTON 3 DISTRICT (589) (closed)
            - PRESTON 4 DISTRICT (556) (closed)
            - PUBLIC DEPARTMENTS 1 DISTRICT (940)
            - PUTNEY DISTRICT (590) (closed)
            - RAWTENSTALL DISTRICT (591) (closed)
            - READING 2 DISTRICT (593) (closed)
            - READING 3 DISTRICT (609) (closed)
            - READING 5 DISTRICT (603) (closed)
            - RECEIVABLES EMPLOYER UNIT (013)
            - REDDITCH DISTRICT (612) (closed)
            - REDHILL 1 DISTRICT (594) (closed)
            - REDHILL 2 DISTRICT (611) (closed)
            - REDRUTH INTEGRATED OFFICE (595)
            - RETFORD DISTRICT (597) (closed)
            - RHYL DISTRICT (598) (closed)
            - RIPON DISTRICT (601) (closed)
            - RIPON RECOVERY (055) (closed)
            - ROCHDALE 1 DISTRICT (602) (closed)
            - ROCHDALE 2 DISTRICT (604) (closed)
            - ROCHDALE DISTRICT (616) (closed)
            - ROMFORD 1 DISTRICT (605) (closed)
            - ROMFORD 2 DISTRICT (608) (closed)
            - ROTHERHAM DISTRICT (606) (closed)
            - ROTHESAY DISTRICT (920) (closed)
            - RUGBY DISTRICT (607) (closed)
            - RUISLIP DISTRICT (600) (CLOSED)
            - SALISBURY DISTRICT (643) (closed)
            - SCARBOROUGH INTEGRATED OFFICE (644) (closed)
            - SCO SOLIHULL (489)
            - SCOTLAND CENTRAL AREA (COMPLIANCE) (817)
            - SCOTLAND CENTRAL AREA (SERVICE) (852)
            - SCOTLAND EAST AREA (COMPLIANCE) (825)
            - SCOTLAND EAST AREA (LONDON SA) (865)
            - SCOTLAND EAST AREA (SERVICE) (837)
            - SCOTLAND NORTH AREA (COMPLIANCE) (795)
            - SCOTLAND NORTH AREA (SERVICE) (985)
            - SCOTLAND SOUTH AREA (COMPLIANCE) (809)
            - SCOTLAND WEST AREA (COMPLIANCE) (820)
            - SCOTLAND WEST AREA (CORPORATION TAX) (808)
            - SCOTLAND WEST AREA (SERVICE) (799)
            - SEFTON AREA (CLAIMS) (965)
            - SEFTON AREA (COMPLIANCE) (077)
            - SEFTON AREA (LONDON SA) (840)
            - SEFTON AREA (SERVICE) (083)
            - SEFTON AREA (SERVICE) (992)
            - SELBY DISTRICT (645) (closed)
            - SHEFFIELD  1 DISTRICT (646) (closed)
            - SHEFFIELD  2 DISTRICT (647) (closed)
            - SHEFFIELD  3 DIST. (726) (closed)
            - SHEFFIELD  3 DISTRICT (648) (closed)
            - SHEFFIELD  4 DIST. (649) (closed)
            - SHEFFIELD  4 DISTRICT (727) (closed)
            - SHEFFIELD  5 DISTRICT (650) (closed)
            - SHEFFIELD  6 DISTRICT (651) (closed)
            - SHEFFIELD RECOVERY (412)
            - SHIPLEY RMS (ACCOUNTING AND PAYMENTS) (901)
            - SHREWSBURY DISTRICT (657) (closed)
            - SITTINGBOURNE DISTRICT (658) (closed)
            - SKIPTON DISTRICT (659) (closed)
            - SLOUGH 2 DISTRICT (692) (closed)
            - SLOUGH RECOVERY (009) (CLOSED)
            - SOHO 2 DISTRICT (661) (closed)
            - SOHO 3 DISTRICT (619) (closed)
            - SOLIHULL 2 DISTRICT (654) (closed)
            - SOLIHULL DISTRICT (220) (closed)
            - SOMERSET AREA (COMPLIANCE) (705)
            - SOMERSET AREA (SERVICE) (794)
            - SOMERSET RECOVERY (086)
            - SOUTH 1 DISTRICT (814) (closed)
            - SOUTH EAST LONDON AREA (COMPLIANCE) (224)
            - SOUTH EAST LONDON AREA (SERVICE) (222)
            - SOUTH ESSEX AREA (COMPLIANCE) (665)
            - SOUTH ESSEX AREA (SERVICE) (662)
            - SOUTH LONDON AREA (Corporation Tax Only) (201)
            - SOUTH WALES AREA (COMPLIANCE) (204)
            - SOUTH WALES AREA (OXFORD) (075)
            - SOUTH WALES AREA (SERVICE) (LOCAL) (948)
            - SOUTH WALES RECOVERY (154)
            - SOUTH WEST LONDON AREA (CT Only) (714)
            - SOUTH WEST LONDON AREA (KINGS U THAMES) (599)
            - SOUTH YORKSHIRE AREA (COMPLIANCE) (725)
            - SOUTH YORKSHIRE AREA (SERVICE) (673)
            - SOUTHAMPTON 1 DISTRICT (718) (closed)
            - SOUTHAMPTON 2 DISTRICT (719) (closed)
            - SOUTHAMPTON 5 DISTRICT (674) (closed)
            - SOUTHAMPTON RECOVERY (016)
            - SOUTHEND 3 DISTRICT (667) (closed)
            - SOUTHGATE DISTRICT (297) (closed)
            - SOUTHPORT 1 DISTRICT (668) (closed)
            - SOUTHPORT 2 DISTRICT (669) (closed)
            - SOUTHPORT INTEGRATED OFFICE (618) (closed)
            - SOUTHPORT RECOVERY (102)
            - SOUTHWARK DISTRICT (622) (closed)
            - SPALDING DISTRICT (675) (closed)
            - SPECIAL TRADE INVESTIGATION UNIT (997)
            - ST ALBANS DISTRICT (634) (closed)
            - ST ANNES DISTRICT (635) (closed)
            - ST AUSTELL 1 DISTRICT (636) (closed)
            - ST AUSTELL 2 (EX BARNSTAPLE 2) (967)
            - ST AUSTELL 2 (EX PLYMOUTH 3) (968)
            - ST AUSTELL 2 (EX SOUTHAMPON 6) (966)
            - ST GILES DISTRICT (638) (closed)
            - ST HELENS  1 DISTRICT (639) (closed)
            - ST HELENS DISTRICT (640) (closed)
            - ST MARTINS DISTRICT (641) (closed)
            - STAFFORD 1 DISTRICT (677) (closed)
            - STAFFORD 2 DISTRICT (678) (closed)
            - STAFFORD INTEGRATED OFFICE (679) (closed)
            - STAFFORDSHIRE AREA (COMPLIANCE) (687)
            - STAFFORDSHIRE AREA (SERVICE) (586)
            - STAINES DISTRICT (633) (closed)
            - STAMPS OFFICE MANCHESTER (032)
            - STEVENAGE DISTRICT (614) (closed)
            - STIRLING DISTRICT (926) (closed)
            - STIRLING RECOVERY (807) (CLOSED)
            - STOCKPORT 1 DISTRICT (681) (closed)
            - STOCKPORT 3 DISTRICT (683) (closed)
            - STOCKPORT 4 DISTRICT (684) (closed)
            - STOCKPORT RECOVERY (413)
            - STOCKTON-ON-TEES DISTRICT (685) (closed)
            - STOKE-ON-TRENT 2 DIST. (629) (closed)
            - STOKE-ON-TRENT 2 DISTRICT (620) (closed)
            - STOKE-ON-TRENT 3 DIST. (630) (closed)
            - STOKE-ON-TRENT 3 DISTRICT (720) (closed)
            - STOKE-ON-TRENT RECOVERY (414)
            - STOURBRIDGE DIST. (688) (closed)
            - STRATFORD 1 DISTRICT (690) (closed)
            - STRATFORD 1 DISTRICT (707) (closed)
            - STRATFORD 2 DISTRICT (691) (closed)
            - STRATFORD RECOVERY (093)
            - STRATFORD-ON-AVON DISTRICT (617) (closed)
            - STREATHAM DISTRICT (694) (closed)
            - STROUD DISTRICT (695) (CLOSED)
            - SUDBURY DISTRICT (696) (closed)
            - SUFFOLK AND N ESSEX AREA (COMPLIANCE) (346)
            - SUFFOLK AND N ESSEX AREA (SERVICE) (245)
            - SUNDERLAND 1 DIST. (697) (closed)
            - SUNDERLAND 1 DISTR.  (625) (closed)
            - SURBITON DISTRICT/STIU (632) (closed)
            - SURREY AND N HAMPSHIRE AREA (COMPLIANCE) (738)
            - SURREY AND N HAMPSHIRE AREA (SERVICE) (765)
            - SURREY RECOVERY (137)
            - SUSSEX AREA (COMPLIANCE) (333)
            - SUSSEX AREA (WORTHING) (334)
            - SUSSEX EAST INTEGRATED OFFICE (234) (closed)
            - SUSSEX RECOVERY (133)
            - SUTTON DISTRICT (698) (closed)
            - SWANSEA 1 DISTRICT (699) (closed)
            - SWINDON 2 DISTRICT (704) (closed)
            - SWINDON GODDARD TDO (703) (CLOSED)
            - SWINDON RECOVERY (007) (closed)
            - Scotland and South Area (Service) (801)
            - TAUNTON 2 DISTRICT (706) (closed)
            - TAUNTON RECOVERY (060) (closed)
            - TEES VALLEY AREA (COMPLIANCE) (417)
            - TEES VALLEY AREA (SERVICE) (406)
            - TEES VALLEY RECOVERY (358)
            - TELFORD 1 TSO (740) (CLOSED)
            - TELFORD 2 TDO (754)
            - TELFORD RECOVERY (211) (closed)
            - THE LONDON CONSTRUCTION INDUSTRY UNIT (596)
            - TONBRIDGE DISTRICT (708) (closed)
            - TORQUAY DISTRICT (710) (closed)
            - TORQUAY RECOVERY (061) (closed)
            - TOTTENHAM 1 DISTRICT (711) (closed)
            - TOTTENHAM DISTRICT (715) (closed)
            - TUNBRIDGE WELLS DISTRICT (713) (closed)
            - TUNBRIDGE WELLS RECOVERY (111)
            - UXBRIDGE DISTRICT (721) (closed)
            - W LANCS AND W CHESHIRE AREA (COMPLIANCE) (750)
            - W LANCS AND W CHESHIRE AREA (SERVICE) (709)
            - W YORKSHIRE AND CRAVEN AREA (COMPLIANCE) (100)
            - W YORKSHIRE AND CRAVEN AREA (CPR TEAM) (398)
            - W YORKSHIRE AND CRAVEN AREA (EMPLOYERS) (073)
            - W YORKSHIRE AND CRAVEN AREA (SERVICE ) (072)
            - WAKEFIELD DISTRICT (724) (closed)
            - WALLASEY DISTRICT (742) (closed)
            - WALSALL 1 DISTRICT (743) (closed)
            - WALSALL 2 DISTRICT (744) (closed)
            - WALSALL 2 TDO (745) (CLOSED)
            - WALTHAMSTOW RECOVERY (159)
            - WALTON-ON-THAMES DISTRICT (731) (CLOSED)
            - WANDSWORTH  RECOVERY (012) (closed)
            - WANDSWORTH 1 TSO (729) (closed)
            - WANDSWORTH DISTRICT (748) (closed)
            - WARRINGTON 1 DISTRICT (749) (closed)
            - WARRINGTON RECOVERY (463) (CLOSED)
            - WARWICKSHIRE COVENTRY AREA (COMPLIANCE) (195)
            - WARWICKSHIRE COVENTRY AREA (SERVICE) (190)
            - WATERLOO 2 (019) (closed)
            - WATFORD 1 DISTRICT (752) (closed)
            - WATFORD 2 DISTRICT (728) (closed)
            - WATFORD RECOVERY (008)
            - WEAR AND SOUTH TYNE AREA (COMPLIANCE) (626)
            - WEAR AND SOUTH TYNE AREA (SERVICE) (465)
            - WELBECK DISTRICT (736) (closed)
            - WELLINGBOROUGH DISTRICT (753) (closed)
            - WELLS DISTRICT (755) (closed)
            - WELSHPOOL DISTRICT (756) (closed)
            - WEMBLEY DISTRICT (757) (closed)
            - WEST BROMWICH DISTRICT (758) (closed)
            - WEST CHESHIRE RECOVERY (430)
            - WEST HAMPSHIRE AREA (COMPLIANCE) (664)
            - WEST HAMPSHIRE AREA (SERVICE) (663)
            - WEST KENT AREA (COMPLIANCE) (579)
            - WEST KENT AREA (SERVICE) (577)
            - WEST LONDON AREA (CHARLES HOUSE)(COMP) (281)
            - WEST LONDON AREA (EALING B'DWY TAX SHOP) (193)
            - WEST MIDLANDS LBO (017) DISTRICT (closed)
            - WEST MIDLANDS LBO (CT) (054)
            - WEST MIDLANDS LBO DISTRICT (468) (closed)
            - WEST MIDLANDS LBO DISTRICT(368) (closed)
            - WEST MIDLANDS LBO DISTRICT(447) (closed)
            - WEST MIDLANDS LBO DISTRICT(448) (closed)
            - WEST WALES AREA (COMPLIANCE) (700)
            - WEST WALES AREA (SERVICE) (615)
            - WEST WALES RECOVERY (162)
            - WESTMINSTER AREA (Corporation Tax Only) (571)
            - WESTMINSTER AREA (SERVICE) (192)
            - WESTON-SUPER-MARE DISTRICT (764) (closed)
            - WEY VALLEY WEST TDO (275) (CLOSED)
            - WHITECHAPLE DISTRICT (766) (closed)
            - WHITEHAVEN DISTRICT (767) (closed)
            - WICK DISTRICT (936) (CLOSED)
            - WIDER ACCESS OFFICE (878)
            - WIDNES DISTRICT (730)
            - WIGAN 2 DISTRICT (734) (closed)
            - WIGAN DISTRICT (732)
            - WILLESDEN DISTRICT (771) (closed)
            - WIMBLEDON DISTRICT (772) (closed)
            - WINCHESTER DISTRICT (773) (closed)
            - WINCHESTER RECOVERY (062) (CLOSED)
            - WINDSOR DISTRICT (774) (closed)
            - WIRRAL INTEGRATED OFFICE (091) (closed)
            - WITHAM DISTRICT (775) (closed)
            - WOKING 1 DISTRICT (776) (closed)
            - WOKING 3 DISTRICT (769) (closed)
            - WOLVERHAMPTON 1 DISTRICT (777) (closed)
            - WOLVERHAMPTON 3 DISTRICT (779) (closed)
            - WOLVERHAMPTON 4 DISTRICT (787) (closed)
            - WOLVERHAMPTON RECOVERY (212)
            - WOOD GREEN DISTRICT (780) (closed)
            - WOODFORD DISTRICT (760) (closed)
            - WOOLWICH 1 (781) (CLOSED)
            - WOOLWICH 2 (CT ONLY) (782) (closed)
            - WORCESTER AND HEREFORD AREA (COMPLIANCE) (792)
            - WORCESTER AND HEREFORD AREA (REDDITCH) (064)
            - WORCESTER RECOVERY (236) (closed)
            - WORKINGTON DISTRICT (784) (closed)
            - WORTHING 1 DISTRICT (785) (closed)
            - WORTHING 2 DISTRICT (735) (closed)
            - WREXHAM DISTRICT (786) (closed)
            - WREXHAM VALUATION (206) (closed)
            - YEOVIL DISTRICT (789) (closed)
            - YORK 1 DISTRICT (790) (closed)
            - YORK RECOVERY (364)
        payableAmount:
          description: The payable amount of money, required by a taxpayer, benefit scheme, or employer towards National Insurance and PAYE.
          example: 10.56
          maximum: 99999999999999.98
          minimum: -99999999999999.98
          multipleOf: 0.01
          type: number
        paymentAmount:
          description: Denotes the total amount for a given payment.
          example: 10.56
          maximum: 99999999999999.98
          minimum: -99999999999999.98
          multipleOf: 0.01
          type: number
    errorResourceObj_400:
      type: object
      required:
        - code
        - reason
      properties:
        reason:
          minLength: 1
          description: Displays the reason of the failure.
          type: string
          maxLength: 120
        code:
          description:
            "The error code representing the error that has occurred. Valid
            values are
400.1 - Constraint violation (Followed by 'Invalid/Missing
            input parameter path.to.field'
400.2 - HTTP message not readable;"
          type: string
          enum:
            - '400.1'
            - '400.2'
    errorResponse_400:
      description: Error Response Payload for this API
      title: Error Response
      type: object
      properties:
        failures:
          $ref: '#/components/schemas/errorResponseFailure_400'
    errorResponseFailure_400:
      description: Array of Error Response Failure Object in Error Response.
      title: Failure Object in Error Response
      type: array
      items:
        $ref: '#/components/schemas/errorResourceObj_400'
    errorResourceObj_403_Forbidden:
      title: 403_Forbidden
      type: object
      required:
        - code
        - reason
      properties:
        reason:
          description: Displays the reason of the failure.
          type: string
          enum:
            - Forbidden
        code:
          description: 'The error code representing the Forbidden Error. '
          type: string
          enum:
            - '403.2'
    errorResponse_403:
      oneOf:
        - $ref: '#/components/schemas/errorResourceObj_403_Forbidden'
      description: Error Response Payload for this API
      title: Forbidden Error Response
`;